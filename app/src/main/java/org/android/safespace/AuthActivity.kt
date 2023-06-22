package org.android.safespace

import android.content.SharedPreferences
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.SignatureException
import java.security.UnrecoverableEntryException
import java.util.Base64
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


// Credits to https://gist.github.com/JosiasSena/3bf4ca59777f7dedcaf41a495d96d984 for KeyStore Help
class AuthActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences
    private var flagSetPin = false
    private var pinFirstAttempt: String? = null
    private val alias = "USER_KEY"
    private val ivAlias = "IV"
    private val androidKeyStore = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"
    private var encryptedKey: String? = null
    private lateinit var keyStore: KeyStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        keyStore = KeyStore.getInstance(androidKeyStore).apply {
            load(null)
        }

        val authButton = findViewById<Button>(R.id.loginButton)
        val appPin = findViewById<TextInputLayout>(R.id.appPin)
        val pinGuide = findViewById<TextView>(R.id.pinGuide)

        sharedPref = getPreferences(MODE_PRIVATE)
        encryptedKey = sharedPref.getString(alias, null)

        if (encryptedKey.isNullOrEmpty()) {
            flagSetPin = true
        }

        if (flagSetPin) {
            authButton.text = getString(R.string.set_pin_continue)
            pinGuide.text = getString(R.string.set_new_pin)
        }

        authButton.setOnClickListener {
            if (appPin.editText?.text!!.isEmpty()) {
                appPin.error = getString(R.string.pin_error_empty)

            } else {
                appPin.error = null

                if (flagSetPin) {
                    if (pinFirstAttempt.isNullOrEmpty() || pinFirstAttempt.isNullOrBlank()) {
                        pinFirstAttempt = appPin.editText?.text.toString()
                        authButton.text = getString(R.string.set_new_pin)
                        pinGuide.text = getString(R.string.confirm_new_pin)
                        appPin.editText?.text!!.clear()
                    } else {
                        if (pinFirstAttempt == appPin.editText?.text.toString()) {

                            storePin(encryptUserPin(pinFirstAttempt))

                            authButton.text = getString(R.string.auth_login)
                            pinGuide.text = getString(R.string.pin)
                            appPin.editText?.text!!.clear()
                            flagSetPin = false

                            Toast.makeText(
                                applicationContext,
                                getString(R.string.pin_setup_confirm),
                                Toast.LENGTH_LONG
                            ).show()

                        } else {
                            authButton.text = getString(R.string.set_pin_continue)
                            pinGuide.text = getString(R.string.pin_error_match)
                            appPin.editText?.text!!.clear()
                        }
                        pinFirstAttempt = null
                    }
                } else {
                    authenticate(appPin.editText?.text.toString())
                }

            }
        }

    }

    private fun storePin(encryptedPin: ByteArray) {
        val editor = sharedPref.edit()
        editor.putString(alias, encryptedPin.toString())
        editor.apply()
    }

    @Throws(
        UnrecoverableEntryException::class,
        NoSuchAlgorithmException::class,
        KeyStoreException::class,
        NoSuchProviderException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class,
        IOException::class,
        InvalidAlgorithmParameterException::class,
        SignatureException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    private fun encryptUserPin(pin: String?): ByteArray {
        val cipher: Cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, setKey())

        val iv: ByteArray = cipher.iv
        val editor = sharedPref.edit()
        editor.putString(ivAlias, Base64.getEncoder().encodeToString(iv))
        editor.apply()

        return cipher.doFinal(pin.toString().toByteArray())
    }

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class,
        NoSuchProviderException::class,
        IOException::class
    )
    private fun setKey(): SecretKey {
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStore)

        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun authenticate(pin: String): Boolean {

        try {
            val cipher = Cipher.getInstance(transformation)

            val iv = Base64.getDecoder().decode(sharedPref.getString(ivAlias, ""))

            val spec = GCMParameterSpec(128, iv)

            cipher.init(
                Cipher.DECRYPT_MODE,
                (keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey,
                spec
            )

            if (pin == String(cipher.doFinal(pin.toByteArray()))) {
                return true
            }

        } catch (e: Exception) {
            return false
        }
        return false

    }

}