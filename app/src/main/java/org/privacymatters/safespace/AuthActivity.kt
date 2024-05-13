package org.privacymatters.safespace

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.privacymatters.safespace.experimental.main.MainnActivity
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.EncPref
import org.privacymatters.safespace.utils.RootCheck
import org.privacymatters.safespace.utils.SetTheme
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
    private var loginBlockedTime: Long = Constants.DEF_NUM_FLAG


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

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        // Experimental - Remove before release
        val intent = Intent(applicationContext, MainnActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()


        authTouch = findViewById(R.id.fingerprint)
        pinField = findViewById(R.id.editTextPassword)

        loginBlockMsg = findViewById(R.id.loginBlockMsg)
        loginBlockTimer = findViewById(R.id.loginBlockTimer)
        loginBlockedTime =
            sharedPref.getLong(Constants.TIME_TO_UNLOCK_START, Constants.DEF_NUM_FLAG)

        // check if user is blocked from login
        val isLoginUnlocked = checkLoginUnlocked() // returns a pair(is blocked?, for how long)

        // if user is blocked, start a countdown
        if (!isLoginUnlocked.first) {
            setLoginBlockMsg()
            countDown(isLoginUnlocked.second).start()
        }

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
                    if (isHardPinSet && biometricPossible && isLoginUnlocked.first) initiateBiometricAuthentication()
                }
            }

            authTouch.setOnClickListener {
                if (biometricPossible && isHardPinSet) biometricPrompt.authenticate(
                    promptInfo
                )
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
        if (pinField.text.toString() == EncPref.getString(
                Constants.HARD_PIN,
                applicationContext
            )
        ) {
            attemptCount = 0

            blockBiometric(false, 0)

            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        } else {
            pinField.error = getString(R.string.pin_error5)
            pinField.setText("")
            attemptCount += 1
            when (attemptCount) {
                10 -> {
                    blockLogin(300000) // 5 minutes
                    countDown(300000).start()
                }

                12 -> {
                    blockLogin(600000) // 10 minutes
                    countDown(600000).start()
                }

                14 -> {
                    blockLogin(1800000) // 30 minutes
                    countDown(1800000).start()
                }

                16 -> {
                    blockLogin(3600000) // 1 Hour
                    countDown(3600000).start()
                }

                18 -> {
                    blockLogin(10800000) // 3 hours
                    countDown(10800000).start()
                }

                20 -> {
                    blockLogin(86400000) // 1 Day
                    countDown(86400000).start()
                    attemptCount = 0
                }
            }

        }
    }

    private fun blockBiometric(flag: Boolean, blockDuration: Long) {
        if (flag) {

            val biometricBackup = sharedPref.getBoolean(Constants.USE_BIOMETRIC, false)

            sharedPref.edit()
                .putLong(Constants.TIME_TO_UNLOCK_START, blockDuration + System.currentTimeMillis())
                .putBoolean(
                    Constants.USE_BIOMETRIC_BCKP,
                    biometricBackup
                )
                .putBoolean(Constants.USE_BIOMETRIC, false)
                .apply()
        } else {
            val biometricRestore = sharedPref.getBoolean(Constants.USE_BIOMETRIC_BCKP, false)

            sharedPref.edit().putBoolean(
                Constants.USE_BIOMETRIC,
                biometricRestore
            ).apply()
        }
    }

    private fun blockLogin(blockDuration: Long) {
        setLoginBlockMsg()

        biometricPossible = false

        blockBiometric(true, blockDuration)
    }

    private fun unlockLogin() {
        removeLoginBlockMsg()
        sharedPref.edit()
            .putLong(Constants.TIME_TO_UNLOCK_DURATION, Constants.DEF_NUM_FLAG)
            .putLong(Constants.TIME_TO_UNLOCK_START, Constants.DEF_NUM_FLAG)
            .apply()
    }

    private fun checkLoginUnlocked(): Pair<Boolean, Long> {

        if (System.currentTimeMillis() > loginBlockedTime) {
            return Pair(true, -1)
        }

        return Pair(false, loginBlockedTime - System.currentTimeMillis())
    }

    private fun countDown(millisRemaining: Long): CountDownTimer {

        return object : CountDownTimer(millisRemaining, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val minutesUntilFinished = (millisUntilFinished / 60000)
                val secondsUntilFinished = (millisUntilFinished % 60000) / 1000

                var remainingTime = ""

                remainingTime += if (minutesUntilFinished < 10) {
                    "0$minutesUntilFinished"
                } else {
                    minutesUntilFinished.toString()
                }

                remainingTime += ":"

                remainingTime += if (secondsUntilFinished < 10) {
                    "0$secondsUntilFinished"
                } else {
                    secondsUntilFinished.toString()
                }

                loginBlockTimer.text = remainingTime
            }

            override fun onFinish() {
                unlockLogin()
                loginBlockTimer.text = ""
            }
        }

    }

    private fun setLoginBlockMsg() {
        loginBlockMsg.text = getString(R.string.pin_error6)
        pinField.isEnabled = false
        authTouch.isEnabled = false
    }

    private fun removeLoginBlockMsg() {
        loginBlockMsg.text = ""
        pinField.isEnabled = true
        authTouch.isEnabled = true
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