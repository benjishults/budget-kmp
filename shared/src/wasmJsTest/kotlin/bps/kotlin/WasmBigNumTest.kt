package bps.kotlin

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlin.math.pow
import kotlin.math.roundToLong

// NOTE for some reason, this test doesn't run for me in intellij: complains about a missing JVM
//      However, I ran it in another project and is passes.
class WasmBigNumTest : FreeSpec() {

    private fun format(double: Double, scale: Int): String {
        val longString = (double * 10.0.pow(scale)).roundToLong().toString()
        return buildString {
            append(longString.substring(0, longString.length - scale))
            append(".")
            append(longString.substring(longString.length - scale))
        }
    }

    init {
        "test" {
            format(12345.12345, 2) shouldBe "12345.12"
            format(12345.12345, 3) shouldBe "12345.123"
            format(9876.9876, 2) shouldBe "9876.99"
        }
    }

}
