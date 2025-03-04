package bps.kotlin

expect class DecimalWithCents(plainString: String) {
//    operator fun plus(other: BigNum): BigNum
//    operator fun minus(other: BigNum): BigNum
    override fun toString(): String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    fun toPlainString(): String
    fun toDouble(): Double
//    fun setScale(scale: Int): BigNum
}

//expect fun buildBigNum(stringRepresentation: String): BigNum

expect val DecimalWithCents_ZERO: DecimalWithCents
