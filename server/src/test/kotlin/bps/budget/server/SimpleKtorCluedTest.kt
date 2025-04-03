package bps.budget.server

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test

// NOTE this was used to report a problem with asClue and withClue
//      https://github.com/kotest/kotest/issues/4836#issuecomment-2773353127
//      Interestingly, clues don't seem to work inside the `testApplication` even when I just use JUnit.
class SimpleKtorCluedTest {

    @Test
    fun test() {
        testApplication {
            application {
                routing {
                    get("/") {
                        call.respondText("root")
                    }
                }
            }
            val client = createClient {
            }
            "endpoint /".asClue {
                val response: HttpResponse = client.get("/")
                response.status shouldBe HttpStatusCode.OK
                // NOTE uncomment next line to get a failure and see if clue is working
//                response.contentType() shouldBe ContentType.Application.Json.withCharset(Charsets.UTF_8)
                response.bodyAsText() shouldBe "root"
            }
        }
    }
}
