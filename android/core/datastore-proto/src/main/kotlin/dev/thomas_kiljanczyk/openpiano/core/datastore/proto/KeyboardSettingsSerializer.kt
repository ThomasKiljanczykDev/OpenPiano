package dev.thomas_kiljanczyk.openpiano.core.datastore.proto

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object KeyboardSettingsSerializer : Serializer<KeyboardSettings> {

    override val defaultValue: KeyboardSettings = KeyboardSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): KeyboardSettings =
        try {
            KeyboardSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Unable to read KeyboardSettings", exception)
        }

    override suspend fun writeTo(t: KeyboardSettings, output: OutputStream) = t.writeTo(output)
}
