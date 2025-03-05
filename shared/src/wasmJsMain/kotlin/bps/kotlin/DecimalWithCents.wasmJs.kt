package bps.kotlin

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.pow
import kotlin.math.roundToLong

@Serializable(with = DecimalWithCentsSerializer::class)
actual class DecimalWithCents(
    val plainString: String,
) {

    val double: Double = plainString.toDouble()
    private val scale = 2

    actual fun toPlainString(): String =
        plainString

    actual fun toDouble(): Double =
        double

    override fun toString(): String {
        val longString = (double * 10.0.pow(scale)).roundToLong().toString()
        return buildString {
            append(longString.substring(0, longString.length - scale))
            append(".")
            append(longString.substring(longString.length - scale))
        }
    }

}

actual val DecimalWithCents_ZERO: DecimalWithCents =
    DecimalWithCents("0.00")

actual fun buildDecimalWithCents(plainString: String): DecimalWithCents =
    DecimalWithCents(plainString)

actual object DecimalWithCentsSerializer : KSerializer<DecimalWithCents> {
    // Serial names of descriptors should be unique, this is why we advise including app package in the name.
    actual override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("bps.kotlin.DecimalWithCents", PrimitiveKind.DOUBLE)

    actual override fun deserialize(decoder: Decoder): DecimalWithCents =
        if (decoder is JsonDecoder) {
            DecimalWithCents(decoder.decodeJsonElement().jsonPrimitive.content)
        } else {
            DecimalWithCents(decoder.decodeString())
        }

    @OptIn(ExperimentalSerializationApi::class)
    actual override fun serialize(encoder: Encoder, value: DecimalWithCents) {
        if (encoder is JsonEncoder) {
            encoder.encodeJsonElement(JsonUnquotedLiteral(value.plainString))
        } else {
            encoder.encodeString(value.plainString)
        }
    }

}
