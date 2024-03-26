package org.privacymatters.safespace

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.privacymatters.safespace.lib.utils.Constants
import org.privacymatters.safespace.lib.utils.EncPref
import org.privacymatters.safespace.lib.utils.RootCheck
import org.privacymatters.safespace.lib.utils.SetTheme
import org.privacymatters.safespace.main.MainActivity
import java.util.concurrent.Executor


/*

Possible authentication scenarios

* hard pin false, fingerprint false -> set up hard pin  +
* hard pin false, fingerprint true  -> set up hard pin  +
* hard pin true, fingerprint false  -> use hard pin     +
* hard pin true, fingerprint true   -> use fingerprint  +

 */

class AuthActivity : AppCompatActivity() {

    private var biometricPossible = true
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var authTouch: ImageButton
    private lateinit var pinField: EditText
    private lateinit var sharedPref: SharedPreferences
    private var confirmCounter = 0
    private var confirmPIN = "-1"
    private var isHardPinSet = false
    private var attemptCount = 0
    private lateinit var loginBlockMsg: TextView
    private lateinit var loginBlockTimer: TextView
    private var loginBlockedTime: Long = 0L
    private var loginBlockedDuration: Int = 0

    //ToDo: add timer for login blocked
    //      add event for system time change

    override fun onCreate(savedInstanceState: Bundle?) {

        // load theme from preferences
        sharedPref = getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE)

        if (!sharedPref.getBoolean(Constants.USE_BIOMETRIC, false)) {
            biometricPossible = false
        }

        SetTheme.setTheme(
            delegate,
            applicationContext,
            sharedPref.getString(getString(R.string.change_theme), getString(R.string.System))!!
        )

        // check if app pin is set
        isHardPinSet = EncPref.getBoolean(Constants.HARD_PIN_SET, applicationContext)
        loginBlockedTime = sharedPref.getLong(Constants.TIME_TO_UNLOCK_START, -1L)
        loginBlockedDuration = sharedPref.getInt(Constants.TIME_TO_UNLOCK_DURATION, -1)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        authTouch = findViewById(R.id.fingerprint)
        pinField = findViewById(R.id.editTextPassword)

        // Root Check
        if (!isPhoneRooted(pinField.context)) {

            if (isHardPinSet) {
                pinField.hint = getString(R.string.pin_text_box)
            }

            // initialize biometric manager and check if biometrics can be used
            val biometricManager = BiometricManager.from(this)
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {

                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
                BiometricManager.BIOMETRIC_STATUS_UNKNOWN,
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                    biometricPossible = false
                }

                BiometricManager.BIOMETRIC_SUCCESS -> {
                    if (isHardPinSet && biometricPossible) initiateBiometricAuthentication()
                }
            }

            authTouch.setOnClickListener {
                if (biometricPossible && isHardPinSet) biometricPrompt.authenticate(promptInfo)
            }

        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {

        return when (keyCode) {
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (!isHardPinSet) {
                    setUpHardPin()
                } else {
                    authenticateUsingHardPin()
                }
                true
            }

            else -> super.onKeyUp(keyCode, event)
        }
    }

    private fun authenticateUsingHardPin() {
        if (isLoginUnlocked()) {

            if (pinField.text.toString() == EncPref.getString(
                    Constants.HARD_PIN,
                    applicationContext
                )
            ) {
                attemptCount = 0
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                pinField.error = getString(R.string.pin_error5)
                pinField.setText("")
                attemptCount += 1
                when (attemptCount) {
                    in 11..19 -> {
                        blockLogin(5)
                    }

                    in 20..22 -> {
                        blockLogin(10)
                    }

                    in 23..26 -> {
                        blockLogin(30)
                    }

                    in 27..30 -> {
                        blockLogin(60)
                    }

                    in 31..34 -> {
                        blockLogin(180)
                    }

                    in 35..38 /* Noice */ -> {
                        blockLogin(1440)
                        attemptCount = 0
                    }
                }
            }
        }
    }

    private fun blockLogin(time: Int) {
        loginBlockMsg.text = getString(R.string.pin_error6)
        pinField.isEnabled = false
        sharedPref.edit()
            .putInt(Constants.TIME_TO_UNLOCK_DURATION, time)
            .putLong(Constants.TIME_TO_UNLOCK_START, System.currentTimeMillis())
            .apply()
    }

    private fun unlockLogin() {
        pinField.isEnabled = true
    }

    private fun isLoginUnlocked(): Boolean {
        if ((System.currentTimeMillis() - loginBlockedTime) > loginBlockedDuration) {
            unlockLogin()
            return true
        }

        return false
    }

    private fun setUpHardPin() {

        pinField.hint = getString(R.string.set_pin_text)

        if (pinField.text?.isEmpty() == true) {
            pinField.error = getString(R.string.pin_error)
        } else if (pinField.text?.length!! < 4) {
            pinField.error = getString(R.string.pin_error2)
        } else {

            if (confirmCounter == 0) {

                confirmCounter += 1
                pinField.hint = getString(R.string.confirm_pin_text)
                confirmPIN = pinField.text.toString()
                pinField.error = null
                pinField.setText("")

            } else if (confirmCounter == 1) {
                if (confirmPIN != pinField.text.toString()) {
                    pinField.error = getString(R.string.pin_error4)
                    pinField.setText("")
                    pinField.hint = getString(R.string.set_pin_text)
                } else {

                    EncPref.setString(Constants.HARD_PIN, confirmPIN, applicationContext)
                    EncPref.setBoolean(Constants.HARD_PIN_SET, true, applicationContext)

                    finish()
                    startActivity(intent)

                }
                confirmCounter = 0
                confirmPIN = "-1"
            }
        }
    }

    private fun initiateBiometricAuthentication() {
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_login))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setConfirmationRequired(false)
            .setNegativeButtonText(getString(R.string.cancel))
            .setSubtitle(getString(R.string.auth_stuck))
            .build()

        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }

            })

        authTouch.isClickable = true

        if (biometricPossible) {
            // launch automatically on start up
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun isPhoneRooted(localContext: Context): Boolean {
        if (RootCheck.isRooted()) {
            val builder = MaterialAlertDialogBuilder(localContext)

            builder.setTitle(getString(R.string.root_check_title))
                .setCancelable(true)
                .setMessage(getString(R.string.root_check_subtitle))
                .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                    // Dismiss the dialog
                    dialog.dismiss()
                    finish()
                }
            val alert = builder.create()
            alert.show()
            return true
        }
        return false
    }
}