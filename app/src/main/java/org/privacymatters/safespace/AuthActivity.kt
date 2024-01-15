package org.privacymatters.safespace

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.privacymatters.safespace.lib.Constants
import org.privacymatters.safespace.lib.RootCheck
import org.privacymatters.safespace.lib.SetTheme
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
    private var confirmCounter = 0
    private var confirmPIN = -1
    private var isHardPinSet = false
    private lateinit var encPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {

        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        encPref = EncryptedSharedPreferences.create(
            "EncPref",
            masterKeyAlias,
            applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // load theme from preferences
        val sharedPref = getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE)

        SetTheme.setTheme(
            delegate,
            applicationContext,
            sharedPref.getString(getString(R.string.change_theme), getString(R.string.System))!!
        )

        // check if app pin is set
        isHardPinSet = encPref.getBoolean(Constants.HARD_PIN_SET, false)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        authTouch = findViewById(R.id.fingerprint)
        pinField = findViewById(R.id.editTextNumberPassword)

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
                    if (isHardPinSet) initiateBiometricAuthentication()
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

        if (pinField.text.toString().isDigitsOnly() &&
            Integer.parseInt(pinField.text.toString()) == encPref.getInt(Constants.HARD_PIN, -1)
        ) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        } else {
            pinField.error = getString(R.string.pin_error5)
            pinField.setText("")
        }
    }

    private fun setUpHardPin() {

        pinField.hint = getString(R.string.set_pin_text)

        if (pinField.text?.isEmpty() == true) {
            pinField.error = getString(R.string.pin_error)
        } else if (pinField.text?.length!! < 4) {
            pinField.error = getString(R.string.pin_error2)
        } else {

            if (confirmCounter == 0) {
                if (pinField.text.toString().isDigitsOnly()) {
                    confirmCounter += 1
                    pinField.hint = getString(R.string.confirm_pin_text)
                    confirmPIN = Integer.parseInt(pinField.text.toString())
                    pinField.error = null
                    pinField.setText("")
                } else {
                    pinField.error = getString(R.string.pin_error3)
                }

            } else if (confirmCounter == 1) {
                if (confirmPIN.toString() != pinField.text.toString()) {
                    pinField.error = getString(R.string.pin_error4)
                    pinField.setText("")
                    pinField.hint = getString(R.string.set_pin_text)
                } else {
                    encPref.edit()
                        .putInt(Constants.HARD_PIN, confirmPIN)
                        .putBoolean(Constants.HARD_PIN_SET, true)
                        .apply()

                    finish()
                    startActivity(intent)

                }
                confirmCounter = 0
                confirmPIN = -1
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