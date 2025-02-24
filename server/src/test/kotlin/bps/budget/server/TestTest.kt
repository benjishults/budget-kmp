package bps.budget.server

import io.kotest.core.spec.style.FreeSpec
import io.ktor.server.testing.testApplication

class TestTest : FreeSpec() {

    init {
        testApplication {
            application {
                module()
            }
        }
    }
}
