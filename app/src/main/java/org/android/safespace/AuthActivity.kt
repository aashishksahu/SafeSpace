package org.android.safespace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.android.safespace.lib.RootCheck
import java.util.concurrent.Executor

class AuthActivity : AppCompatActivity() {

//    ToDo: Implement authentication time out setting

    private var pinNotPossible = false
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val authButton = findViewById<Button>(R.id.loginButton)

        // Root Check
        isPhoneRooted(authButton.context)

        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                pinNotPossible = true
            }

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                val builder = MaterialAlertDialogBuilder(authButton.context)
                builder.setTitle(getString(R.string.auth_alert_update))
                    .setCancelable(true)
                    .setMessage(getString(R.string.auth_no_biometric_msg))
                    .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                    }
            }

            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                pinNotPossible = true
            }

            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                pinNotPossible = true
            }

            BiometricManager.BIOMETRIC_SUCCESS -> {
                initiateAuthentication()
            }
        }

        authButton.setOnClickListener {
            if (pinNotPossible) {
                val builder = MaterialAlertDialogBuilder(authButton.context)
                builder.setTitle(getString(R.string.auth_alert))
                    .setCancelable(true)
                    .setMessage(getString(R.string.auth_no_biometric_msg))
                    .setPositiveButton(getString(R.string.ok)) { _, _ ->
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    }
                    .setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }.show()

            } else {
                biometricPrompt.authenticate(promptInfo)
            }

        }

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

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        applicationContext, getString(R.string.auth_fail),
                        Toast.LENGTH_LONG
                    ).show()
                }
            })

        if (!pinNotPossible) {
            // launch automatically on start up
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun isPhoneRooted(localContext: Context) {
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
        }
    }
}