package org.privacymatters.safespace.utils

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.privacymatters.safespace.auth.AuthCrypto
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class KVM(
    val instance: Map<String, String>
)

object KVMHelper {

    private val Context.dataStore: DataStore<KVM> by dataStore(
        fileName = "prefs.json",
        serializer = KVMSerializer,
    )

    suspend fun setValue(context: Context, kvmKey: String, value: String) {
        context.dataStore.updateData {
            val newMap = it.instance.toMutableMap()
            newMap[kvmKey] = value
            it.copy(instance = newMap)
        }
    }

    suspend fun getValue(context: Context, kvmKey: String): String {
        return context.dataStore.data.map { it.instance[kvmKey] }.first() ?: ""
    }


    suspend fun setValueEncrypted(context: Context, kvmKey: String, value: String) {
        var keyAlias = getValue(context, "auth_key_alias")

        if (keyAlias.isEmpty()) {
            keyAlias = AuthCrypto.randomAlphanumeric(5)
            setValue(context, "auth_key_alias", keyAlias)
        }

        context.dataStore.updateData {
            val newMap = it.instance.toMutableMap()

            // encrypt value before saving
            val (cipherText, iv) = AuthCrypto.encryptKVMValue(value, keyAlias)

            newMap["${kvmKey}_iv"] = iv.toHexString()
            newMap["${kvmKey}_cipherText"] = cipherText.toHexString()
            it.copy(instance = newMap)
        }
    }

    suspend fun getValueEncrypted(context: Context, kvmKey: String): String? {
        val data = context.dataStore.data.first()
        val keyAlias = data.instance["auth_key_alias"]
        val iv = data.instance["${kvmKey}_iv"]
        val cipherText = data.instance["${kvmKey}_cipherText"]

        if (keyAlias.isNullOrEmpty() || iv.isNullOrEmpty() || cipherText.isNullOrEmpty()) {
            return null
        }

        val decrypted = AuthCrypto.decryptKVMValue(
            cipherText = cipherText.hexToByteArray(),
            iv = iv.hexToByteArray(),
            alias = keyAlias
        )

        return decrypted.decodeToString()
    }
}

private object KVMSerializer : Serializer<KVM> {

    override val defaultValue: KVM = KVM(instance = emptyMap())

    override suspend fun readFrom(input: InputStream): KVM =
        try {
            Json.decodeFromString(
                KVM.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read KVM", serialization)
        }

    override suspend fun writeTo(t: KVM, output: OutputStream) {
        output.write(
            Json.encodeToString(KVM.serializer(), t)
                .encodeToByteArray()
        )
    }

}

