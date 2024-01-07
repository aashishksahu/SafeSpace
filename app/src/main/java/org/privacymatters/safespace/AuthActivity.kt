package org.privacymatters.safespace

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
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

class AuthActivity : AppCompatActivity() {

    private var biometricNotPossible = false
    private lateinit var executor: Executor
    private var passwdField: EditText? = null
    private lateinit var authButton: Button
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var biometricNotPossibleReason: String = ""
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

        val sharedPref = getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE)

        SetTheme.setTheme(
            delegate,
            applicationContext,
            sharedPref.getString(getString(R.string.change_theme), getString(R.string.System))!!
        )

        // check if app pin is set
        val isHardPinSet = encPref.getBoolean(Constants.HARD_PIN_SET, false)
        val isHardPinNeeded = encPref.getBoolean(Constants.HARD_PIN_NEEDED, false)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        authButton = findViewById(R.id.loginButton)

        // Root Check
        if (!isPhoneRooted(authButton.context)) {

            if (isHardPinNeeded) {
                initHardPin(firstUse = false)
            } else {
                val biometricManager = BiometricManager.from(this)
                when (biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) or
                        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {

                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                        biometricNotPossibleReason = "Reason: Biometric Hardware not supported"
                        biometricNotPossible = true
                    }

                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                        biometricNotPossibleReason = "Reason: Biometric Hardware unavailable"
                        biometricNotPossible = true
                    }

                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                        biometricNotPossibleReason = "Reason: Biometric not set up"
                        biometricNotPossible = true
                    }

                    BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                        biometricNotPossibleReason = "Reason: Biometric not supported"
                        biometricNotPossible = true
                    }

                    BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                        biometricNotPossibleReason = "Reason: Biometric status unknown"
                        biometricNotPossible = true
                    }

                    BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                        biometricNotPossibleReason = "Reason: System update required"
                        biometricNotPossible = true
                    }

                    BiometricManager.BIOMETRIC_SUCCESS -> {
                        initiateAuthentication()
                    }
                }
            }

            authButton.setOnClickListener {
                if (biometricNotPossible && !isHardPinSet) {

                    if (confirmCounter == 1) {
                        if (confirmPIN.toString() != passwdField?.text.toString()) {
                            passwdField?.error = getString(R.string.pin_error4)
                            passwdField?.setText("")
                            authButton.text = getString(R.string.set_pin_text)
                        } else {
                            encPref.edit()
                                .putInt(Constants.HARD_PIN, confirmPIN)
                                .putBoolean(Constants.HARD_PIN_SET, true)
                                .putBoolean(Constants.HARD_PIN_NEEDED, true)
                                .apply()

                            finish()
                            startActivity(intent)

                        }
                        confirmCounter = 0
                        confirmPIN = -1
                    } else if (confirmCounter == 0 && passwdField != null) {
                        if (passwdField?.text?.isEmpty() == true) {
                            passwdField?.error = getString(R.string.pin_error)
                        } else if (passwdField?.text?.length!! < 4) {
                            passwdField?.error = getString(R.string.pin_error2)
                        } else {
                            if (passwdField?.text.toString().isDigitsOnly()) {
                                confirmCounter += 1
                                authButton.text = getString(R.string.confirm_pin_text)
                                confirmPIN = Integer.parseInt(passwdField?.text.toString())
                                passwdField?.error = null
                                passwdField?.setText("")
                            } else {
                                passwdField?.error = getString(R.string.pin_error3)
                            }
                        }
                    } else if (passwdField == null) {

                        val errorMsg =
                            getString(R.string.auth_no_biometric_msg) + "\n" + biometricNotPossibleReason

                        val builder = MaterialAlertDialogBuilder(authButton.context)

                        builder.setTitle(getString(R.string.auth_alert))
                            .setCancelable(true)
                            .setMessage(errorMsg)
                            .setPositiveButton(getString(R.string.ok)) { _, _ ->

                                initHardPin(firstUse = true)
                            }
                            .setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                                dialog.dismiss()
                            }.show()
                    }

                } else if (isHardPinNeeded) {
                    if (authenticateUsingHardPin(passwdField?.text.toString())) {
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    } else {
                        passwdField?.error = getString(R.string.pin_error5)
                        passwdField?.setText("")
                    }
                } else {
                    biometricPrompt.authenticate(promptInfo)
                }

            }
        }
    }

    private fun authenticateUsingHardPin(passwd: String): Boolean {

        return passwd.isDigitsOnly() &&
                Integer.parseInt(passwd) == encPref.getInt(Constants.HARD_PIN, -1)
    }

    private fun initHardPin(firstUse: Boolean) {
        if (firstUse) {
            authButton.text = getString(R.string.set_pin_text)
        }
        passwdField = findViewById(R.id.editTextNumberPassword)
        passwdField?.visibility = View.VISIBLE

    }

    private fun initiateAuthentication() {
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

        if (!biometricNotPossible) {
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