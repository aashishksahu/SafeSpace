package org.privacymatters.safespace.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.KVMHelper

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
                encPref!!.edit { putString(Constants.HARD_PIN, oldPin.toString()) }
            }

            // migrate to Jetpack Datastore
            CoroutineScope(Dispatchers.IO).launch {
                val hardPinSetInDataStore =
                    KVMHelper.getValue(applicationContext, Constants.HARD_PIN_SET)

                // if this is true, then pin is migrated to jetpack datastore
                if (hardPinSetInDataStore == "true") return@launch

                val hardPin = encPref!!.getString(Constants.HARD_PIN, "")

                if (hardPin.isNullOrEmpty()) {
                    // this means that password is not set at all (both encPref and datastore)
                    KVMHelper.setValueEncrypted(
                        applicationContext,
                        Constants.HARD_PIN_SET,
                        "false"
                    )
                } else {
                    // here, the password is saved in data store from encPref
                    KVMHelper.setValueEncrypted(
                        applicationContext,
                        Constants.HARD_PIN,
                        hardPin
                    )
                    KVMHelper.setValueEncrypted(
                        applicationContext,
                        Constants.HARD_PIN_SET,
                        "true"
                    )
                }
            }

        }

        fun getBoolean(pref: String, applicationContext: Context): Boolean {
            init(applicationContext)

            val kvmPref = runBlocking { KVMHelper.getValue(applicationContext, pref) == "true" }

            return if (kvmPref)
                true
            else
                encPref!!.getBoolean(pref, false)
        }

        suspend fun setBoolean(pref: String, value: Boolean, applicationContext: Context) {
            init(applicationContext)

            KVMHelper.setValue(applicationContext, pref, value.toString())

//            encPref!!.edit {
//                putBoolean(pref, value)
//            }

        }

        fun getString(pref: String, applicationContext: Context): String? {
            init(applicationContext)

            val kvmPref = runBlocking { KVMHelper.getValue(applicationContext, pref) }

            return kvmPref.ifEmpty { encPref!!.getString(pref, "-1") }

        }


        suspend fun setPassword(pref: String, value: String, applicationContext: Context) {
            init(applicationContext)

            KVMHelper.setValueEncrypted(applicationContext, pref, value)

//            encPref!!.edit {
//                putString(pref, value)
//            }
        }


        suspend fun clearPassword(pref: String, applicationContext: Context) {
            init(applicationContext)

            KVMHelper.setValueEncrypted(applicationContext, pref, "-1")

//            encPref!!.edit {
//                putString(pref, "-1")
//            }
        }

        suspend fun clearBoolean(pref: String, applicationContext: Context) {
            init(applicationContext)

            KVMHelper.setValue(applicationContext, pref, "false")

//            encPref!!.edit {
//                putBoolean(pref, false)
//            }
        }

    }
}