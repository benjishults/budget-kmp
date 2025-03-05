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
import java.math.BigDecimal
import java.math.RoundingMode

@Serializable(with = JvmDecimalWithCentsSerializer::class)
class JvmDecimalWithCents private constructor(
    val bigDecimal: BigDecimal,
) {

    constructor(plainString: String) : this(BigDecimal(plainString).setScale(2, RoundingMode.HALF_UP))

    fun toPlainString(): String =
        bigDecimal.toPlainString()

    fun toDouble(): Double =
        bigDecimal.toDouble()

    override fun toString(): String =
        bigDecimal.toPlainString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JvmDecimalWithCents) return false

        if (bigDecimal != other.bigDecimal) return false

        return true
    }

    override fun hashCode(): Int {
        return bigDecimal.hashCode()
    }

}

object JvmDecimalWithCentsSerializer : KSerializer<JvmDecimalWithCents> {
    // Serial names of descriptors should be unique, this is why we advise including app package in the name.
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("bps.kotlin.DecimalWithCents", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): JvmDecimalWithCents =
        if (decoder is JsonDecoder) {
            JvmDecimalWithCents(decoder.decodeJsonElement().jsonPrimitive.content)
        } else {
            JvmDecimalWithCents(decoder.decodeString())
        }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: JvmDecimalWithCents) {
        val bdString = value.bigDecimal.toPlainString()

        if (encoder is JsonEncoder) {
            encoder.encodeJsonElement(JsonUnquotedLiteral(bdString))
        } else {
            encoder.encodeString(bdString)
        }
    }

}
