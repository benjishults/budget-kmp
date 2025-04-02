package bps.budget.server

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
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

class SimpleKtorTest : FreeSpec() {

    init {
        testApplication {
            application {
                routing {
                    get("/") {
                        call.respondText("root")
                    }
                }
            }
            val client = HttpClient {
            }
            "/" {
                val response: HttpResponse = client.get("/")
                response.status shouldBe HttpStatusCode.OK
                response.contentType() shouldBe ContentType.Text.Plain.withCharset(Charsets.UTF_8)
                response.bodyAsText() shouldBe "root"
            }
        }
    }
}
