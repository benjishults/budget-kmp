package bps.budget.server

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication

// NOTE this test was used to figure out an issue with FreeSpec
//      https://github.com/kotest/kotest/issues/4836
//      https://stackoverflow.com/questions/79542556/simplest-possible-ktor-server-test-host-test-fails-with-connection-refused
class SimpleKtorFreeSpecTest : FreeSpec() {

    init {
        "test endpoints" {
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
                val response: HttpResponse = client.get("/")
                response.status shouldBe HttpStatusCode.OK
                response.contentType() shouldBe ContentType.Text.Plain.withCharset(Charsets.UTF_8)
                response.bodyAsText() shouldBe "root"
            }
        }
    }
}
