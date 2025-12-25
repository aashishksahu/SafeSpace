package org.privacymatters.safespace.utils

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class AuthenticationPin(
    val pinStatus: Boolean,
    val hardPin: String
)

object AuthenticationSerializer : Serializer<AuthenticationPin> {

    override val defaultValue: AuthenticationPin = AuthenticationPin(pinStatus = false, hardPin = "")

    override suspend fun readFrom(input: InputStream): AuthenticationPin =
        try {
            Json.decodeFromString(
                AuthenticationPin.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read AuthenticationPin", serialization)
        }

    override suspend fun writeTo(t: AuthenticationPin, output: OutputStream) {
        output.write(
            Json.encodeToString(AuthenticationPin.serializer(), t)
                .encodeToByteArray()
        )
    }
}
