package org.android.safespace

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.MasterKey
import com.google.android.material.textfield.TextInputLayout
import org.android.safespace.lib.Constants

class AuthActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private var flagSetPin = false
    private var setPinCounter = 0
    private var pinFirstAttempt = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val authButton = findViewById<Button>(R.id.loginButton)
        val appPin = findViewById<TextInputLayout>(R.id.appPin)
        val pinGuide = findViewById<TextView>(R.id.pinGuide)

        sharedPref = getPreferences(MODE_PRIVATE)

        if (!sharedPref.getBoolean(Constants.PIN_SETUP_COMPLETE, true)) {
            flagSetPin = true
        }

        if (flagSetPin) {
            authButton.text = getString(R.string.set_pin_continue)
            pinGuide.text = getString(R.string.set_new_pin)
        }

        authButton.setOnClickListener {
            if (appPin.editText?.text!!.isEmpty()) {
                appPin.error = getString(R.string.pin_error_empty)

            } else if (flagSetPin) {
                if (pinFirstAttempt == 0) {
                    pinFirstAttempt = Integer.parseInt(appPin.editText?.text.toString())
                    authButton.text = getString(R.string.set_new_pin)
                    pinGuide.text = getString(R.string.confirm_new_pin)
                    appPin.editText?.text!!.clear()
                } else {
                    if (pinFirstAttempt == Integer.parseInt(appPin.editText?.text.toString())) {
                        val prefEditor = sharedPref.edit()
                        prefEditor.putBoolean(Constants.PIN_SETUP_COMPLETE, true)
                        prefEditor.apply()
                        authButton.text = getString(R.string.auth_login)
                        pinGuide.text = getString(R.string.pin)
                        appPin.editText?.text!!.clear()
                        flagSetPin = false
                    } else {
                        authButton.text = getString(R.string.set_pin_continue)
                        pinGuide.text = getString(R.string.pin_error_match)
                        appPin.editText?.text!!.clear()
                        pinFirstAttempt = 0
                    }
                }
            }

        }

    }

    private fun setKey() {
        val mainKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private fun getKey() {

    }
}