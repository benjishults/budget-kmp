package bps.kotlin

actual typealias DecimalWithCents = JvmDecimalWithCents

actual val DecimalWithCents_ZERO: DecimalWithCents = JvmDecimalWithCents("0.00")

actual fun buildDecimalWithCents(plainString: String): DecimalWithCents =
    DecimalWithCents(plainString)

actual typealias DecimalWithCentsSerializer = JvmDecimalWithCentsSerializer
