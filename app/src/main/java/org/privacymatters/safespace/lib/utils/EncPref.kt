package org.privacymatters.safespace.lib.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class EncPref {
    companion object {
        private var encPref: SharedPreferences? = null
        private fun init(applicationContext: Context) {

            if (encPref == null) {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

                encPref = EncryptedSharedPreferences.create(
                    "EncPref",
                    masterKeyAlias,
                    applicationContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }

            // migrate to new pin
            val oldPin = encPref!!.getInt(Constants.HARD_PIN, -1)

            if (oldPin != -1) {
                encPref!!.edit().putString(Constants.HARD_PIN, oldPin.toString()).apply()
            }


        }

        fun getBoolean(pref: String, applicationContext: Context): Boolean {
            init(applicationContext)
            return encPref!!.getBoolean(pref, false)
        }

        fun setBoolean(pref: String, value: Boolean, applicationContext: Context) {
            init(applicationContext)

            encPref!!.edit()
                .putBoolean(pref, value)
                .apply()

        }

        fun getString(pref: String, applicationContext: Context): String? {
            init(applicationContext)
            return encPref!!.getString(pref, "-1")
        }


        fun setString(pref: String, value: String, applicationContext: Context) {
            init(applicationContext)

            encPref!!.edit()
                .putString(pref, value)
                .apply()
        }


        fun clearString(pref: String, applicationContext: Context) {
            init(applicationContext)

            encPref!!.edit()
                .putString(pref, "-1")
                .apply()
        }

        fun clearBoolean(pref: String, applicationContext: Context) {
            init(applicationContext)

            encPref!!.edit()
                .putBoolean(pref, false)
                .apply()
        }

    }
}