package org.privacymatters.safespace.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.runBlocking
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.KVMHelper

class EncPref {
    companion object {
        private var encPref: SharedPreferences? = null

        private var initialized = false

        private fun init(applicationContext: Context) {

            if (initialized) return

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
            // Shared preferences runs on main thread, jetpack datastore runs on IO thread,
            // can't redesign the entire auth flow because Google felt cute one day and changed things.
            // Hence, "runBlocking". It won't cause any visible performance lag

            runBlocking {
                val hardPinSetInDataStore =
                    KVMHelper.getValue(applicationContext, Constants.HARD_PIN_SET)

                // if this is true, then pin is migrated to jetpack datastore
                if (hardPinSetInDataStore == "true") return@runBlocking

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
                    KVMHelper.setValue(
                        applicationContext,
                        Constants.HARD_PIN_SET,
                        "true"
                    )
                }
            }
            initialized = true
        }

        fun getPasswordStatus(applicationContext: Context): Boolean {
            init(applicationContext)

            return runBlocking {
                KVMHelper.getValue(
                    applicationContext,
                    Constants.HARD_PIN_SET
                ) == "true"
            }

        }

        fun setPasswordStatus(value: Boolean, applicationContext: Context) {
            init(applicationContext)

            runBlocking {
                KVMHelper.setValue(applicationContext, Constants.HARD_PIN_SET, value.toString())
            }

        }

        fun getPassword(applicationContext: Context): String? {
            init(applicationContext)

            return runBlocking {
                KVMHelper.getValueEncrypted(
                    applicationContext,
                    Constants.HARD_PIN
                )
            }

        }


        fun setPassword(value: String, applicationContext: Context) {
            init(applicationContext)

            runBlocking {
                KVMHelper.setValueEncrypted(applicationContext, Constants.HARD_PIN, value)
            }

        }


        fun clearPassword(applicationContext: Context) {
            init(applicationContext)

            runBlocking {
                KVMHelper.setValueEncrypted(applicationContext, Constants.HARD_PIN, "-1")
            }

        }

        fun clearPasswordStatus(applicationContext: Context) {
            init(applicationContext)

            runBlocking {
                KVMHelper.setValue(applicationContext, Constants.HARD_PIN_SET, "false")
            }
        }

    }
}