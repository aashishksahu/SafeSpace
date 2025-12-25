package org.privacymatters.safespace.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.privacymatters.safespace.utils.KVM
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object AuthCrypto {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    private const val ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    fun randomAlphanumeric(length: Int): String {
        val random = SecureRandom()
        return buildString(length) {
            repeat(length) {
                append(ALPHANUM[random.nextInt(ALPHANUM.length)])
            }
        }
    }


    private fun createKeyIfNotExists(alias: String) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        if (keyStore.containsAlias(alias)) return

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keySpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keySpec)
        keyGenerator.generateKey()
    }

    private fun getSecretKey(alias: String): SecretKey {

        createKeyIfNotExists(alias)

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
        return keyStore.getKey(alias, null) as SecretKey
    }

    fun encryptKVMValue(
        payload: String,
        keyAlias: String
    ): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(keyAlias))

        val cipherText = cipher.doFinal(payload.toByteArray())
        val iv = cipher.iv

        return Pair(cipherText, iv)
    }

    fun decryptKVMValue(
        cipherText: ByteArray,
        iv: ByteArray,
        alias: String
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)

        cipher.init(
            Cipher.DECRYPT_MODE,
            getSecretKey(alias),
            spec
        )

        return cipher.doFinal(cipherText)
    }


}