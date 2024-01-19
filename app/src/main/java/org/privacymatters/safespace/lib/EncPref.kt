package org.privacymatters.safespace.lib

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
        }

        fun getBoolean(pref: String, applicationContext: Context): Boolean {
            init(applicationContext)
            return encPref!!.getBoolean(pref, false)
        }

        fun getInt(pref: String, applicationContext: Context): Int {
            init(applicationContext)
            return encPref!!.getInt(pref, -1)
        }


        fun setInt(pref: String, value: Int, applicationContext: Context) {
            init(applicationContext)

            encPref!!.edit()
                .putInt(pref, value)
                .apply()
        }

        fun setBoolean(pref: String, value: Boolean, applicationContext: Context) {
            init(applicationContext)

            encPref!!.edit()
                .putBoolean(pref, value)
                .apply()

        }

        fun clearInt(pref: String, applicationContext: Context) {
            init(applicationContext)

            encPref!!.edit()
                .putInt(pref, -1)
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