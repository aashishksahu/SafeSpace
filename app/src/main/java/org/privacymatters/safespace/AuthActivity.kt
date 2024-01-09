package org.privacymatters.safespace

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
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

* fingerprint false, hard pin false -> set up hard pin  +
* fingerprint false, hard pin true  -> use hard pin     +
* fingerprint true,  hard pin false -> use fingerprint
* fingerprint true,  hard pin true  -> use fingerprint

 */

class AuthActivity : AppCompatActivity() {

    private var biometricPossible = true
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var authButton: Button
    private lateinit var authTouch: ImageButton
    private lateinit var pinField: EditText
    private var confirmCounter = 0
    private var confirmPIN = -1
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
        val isHardPinSet = encPref.getBoolean(Constants.HARD_PIN_SET, false)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        //login button
        authButton = findViewById(R.id.loginButton)
        authTouch = findViewById(R.id.fingerprint)
        pinField = findViewById(R.id.editTextNumberPassword)

        // Root Check
        if (!isPhoneRooted(authButton.context)) {

            // initialize biometric manager and check if biometrics can be used
            val biometricManager = BiometricManager.from(this)
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) or
                    biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {

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

            authButton.setOnClickListener {

                if (!isHardPinSet) {
                    setUpHardPin()
                } else {
                    authenticateUsingHardPin()
                }
            }

            authTouch.setOnClickListener {
                if (biometricPossible) biometricPrompt.authenticate(promptInfo)
            }

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

        authButton.text = getString(R.string.set_pin_text)

        if (pinField.text?.isEmpty() == true) {
            pinField.error = getString(R.string.pin_error)
        } else if (pinField.text?.length!! < 4) {
            pinField.error = getString(R.string.pin_error2)
        } else {

            if (confirmCounter == 0) {
                if (pinField.text.toString().isDigitsOnly()) {
                    confirmCounter += 1
                    authButton.text = getString(R.string.confirm_pin_text)
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
                    authButton.text = getString(R.string.set_pin_text)
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
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .setConfirmationRequired(false)
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