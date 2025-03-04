package bps.kotlin

import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.roundToLong

@Serializable
actual class DecimalWithCents(
    val double: Double,
    val scale: Int = 2,
    val plainString: String = double.toString(),
) {

    actual constructor(plainString: String) : this(plainString.toDouble(), 2, plainString)

//    override fun plus(other: BigNum): BigNum =
//        WasmBigNum(double + (other as WasmBigNum).double)
//
//    override fun minus(other: BigNum): BigNum =
//        WasmBigNum(double - (other as WasmBigNum).double)

    actual fun toPlainString(): String =
        plainString

    actual fun toDouble(): Double =
        double

    actual override fun toString(): String {
        val longString = (double * 10.0.pow(scale)).roundToLong().toString()
        return buildString {
            append(longString.substring(0, longString.length - scale))
            append(".")
            append(longString.substring(longString.length - scale))
        }
    }

//    override fun setScale(scale: Int): BigNum =
//        WasmBigNum(double, scale)

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecimalWithCents) return false

        if (double != other.double) return false

        return true
    }

    actual override fun hashCode(): Int {
        return double.hashCode()
    }
}

actual val DecimalWithCents_ZERO: DecimalWithCents =
    DecimalWithCents(0.0)
