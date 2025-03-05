package bps.kotlin

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = DecimalWithCentsSerializer::class)
expect class DecimalWithCents {
    fun toPlainString(): String
    fun toDouble(): Double
}

expect val DecimalWithCents_ZERO: DecimalWithCents

expect fun buildDecimalWithCents(plainString: String): DecimalWithCents

expect object DecimalWithCentsSerializer: KSerializer<DecimalWithCents> {
    override val descriptor: SerialDescriptor
    override fun serialize(encoder: Encoder, value: DecimalWithCents)
    override fun deserialize(decoder: Decoder): DecimalWithCents
}
