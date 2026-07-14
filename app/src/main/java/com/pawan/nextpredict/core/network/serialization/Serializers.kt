package com.pawan.nextpredict.core.network.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Custom Kotlinx Serialization serializer to handle fields that can return
 * either a numeric Double value, a numeric string, or an empty string "" (which maps to null).
 */
object DoubleAsStringSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DoubleAsStringSerializer", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeDouble(value)
        }
    }

    override fun deserialize(decoder: Decoder): Double? {
        val input = decoder as? JsonDecoder ?: return try {
            decoder.decodeDouble()
        } catch (e: Exception) {
            null
        }
        val primitive = input.decodeJsonElement() as? JsonPrimitive ?: return null
        if (primitive.isString) {
            val str = primitive.content.trim()
            if (str.isEmpty() || str.equals("-", ignoreCase = true)) return null
            // Remove any commas (e.g. "1,200.50")
            val cleanStr = str.replace(",", "")
            return cleanStr.toDoubleOrNull()
        }
        return primitive.doubleOrNull
    }
}

/**
 * Custom serializer to handle Long values that can be numeric, string, or empty.
 */
object LongAsStringSerializer : KSerializer<Long?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LongAsStringSerializer", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Long?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeLong(value)
        }
    }

    override fun deserialize(decoder: Decoder): Long? {
        val input = decoder as? JsonDecoder ?: return try {
            decoder.decodeLong()
        } catch (e: Exception) {
            null
        }
        val primitive = input.decodeJsonElement() as? JsonPrimitive ?: return null
        if (primitive.isString) {
            val str = primitive.content.trim()
            if (str.isEmpty() || str.equals("-", ignoreCase = true)) return null
            val cleanStr = str.replace(",", "")
            return cleanStr.toLongOrNull()
        }
        return primitive.longOrNull
    }
}
