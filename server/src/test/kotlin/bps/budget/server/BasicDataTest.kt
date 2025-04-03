package bps.budget.server

import bps.budget.model.AccountResponse
import bps.budget.model.AccountTransactionsResponse
import bps.budget.model.AccountType
import bps.budget.model.AccountsResponse
import bps.budget.model.TransactionResponse
import bps.budget.persistence.jdbc.JdbcAccountDao
import bps.budget.persistence.jdbc.JdbcTransactionDao
import bps.jdbc.configureDataSource
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class BasicDataTest : AnnotationSpec() {

    // NOTE clues don't seem to be working.
    // NOTE this currently relies on data in the DB left over from CLI tests.  To run this in CI, I will need to create
    //      fresh data with its own budgetId so as not to interfere with other test running.

    @Test
    fun test() {
        val budgetServerConfigurations = BudgetServerConfigurations(sequenceOf("budget-server.yml"))
        val dataSource = configureDataSource(budgetServerConfigurations.jdbc, budgetServerConfigurations.hikari)
        val accountDao = JdbcAccountDao(dataSource)
        val transactionDao = JdbcTransactionDao(dataSource)
        val budgetId: Uuid = Uuid.parse("89bc165a-ee70-43a4-b637-2774bcfc3ea4")
//        val userId = Uuid.parse("f0f209c8-1b1e-43b3-8799-2dba58524d02")
//        val baseUrl = "http://localhost:8085"
        val baseUrl = ""
//        "test endpoints" {
        testApplication {
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) { json() }
            application {
                module(accountDao, transactionDao)
            }
            val client = createClient {
                this.install(ContentNegotiation) { json() }
            }
            "/".asClue {
                val response: HttpResponse = client.get("$baseUrl/")
                response.status shouldBe HttpStatusCode.OK
                response.contentType() shouldBe ContentType.Text.Plain.withCharset(Charsets.UTF_8)
                response.bodyAsText() shouldBe "root"
            }
            "/content/sample.html".asClue {
                val response: HttpResponse = client.get("$baseUrl/content/sample.html")
                response.status shouldBe HttpStatusCode.OK
                response.contentType() shouldBe ContentType.Text.Html.withCharset(Charsets.UTF_8)
                response.bodyAsText() shouldContain "li>Ktor</li"
            }
            "/test".asClue {
                val response: HttpResponse = client.get("$baseUrl/test")
                response.status shouldBe HttpStatusCode.OK
                response.contentType() shouldBe ContentType.Text.Plain.withCharset(Charsets.UTF_8)
                response.bodyAsText() shouldBe "succeeded"
            }
            var generalAccountId: Uuid? = null
            var checkingAccountId: Uuid? = null
            var draftAccountId: Uuid? = null
            var foodAccountId: Uuid? = null
            "/budgets/{budgetId}/accounts".asClue {
                "no type specified".asClue {
                    val response: HttpResponse = client.get("$baseUrl/budgets/${budgetId}/accounts") {
                        this.accept(ContentType.Any)
                    }
                    response.status shouldBe HttpStatusCode.OK
                    response.contentType() shouldBe ContentType.Application.Json.withCharset(Charsets.UTF_8)
                    val body: AccountsResponse = response.body()
                    body.items.size shouldBe 17
                    draftAccountId = body.items.firstOrNull { it.type == AccountType.draft.name }?.id
                    draftAccountId.shouldNotBeNull()
                }
                "type=category".asClue {
                    val response: HttpResponse = client.get("$baseUrl/budgets/${budgetId}/accounts?type=category") {
                        this.accept(ContentType.Application.Json)
                    }
                    response.status shouldBe HttpStatusCode.OK
                    response.contentType() shouldBe ContentType.Application.Json.withCharset(Charsets.UTF_8)
                    val body: AccountsResponse = response.body()
                    body.items.size shouldBe 14
                    generalAccountId = body.items.firstOrNull { it.name == "General" }?.id
                    generalAccountId.shouldNotBeNull()
                    foodAccountId = body.items.firstOrNull { it.name == "Food" }?.id
                    foodAccountId.shouldNotBeNull()
                }
                "type=real".asClue {
                    val response: HttpResponse = client.get("$baseUrl/budgets/${budgetId}/accounts?type=real")
                    response.status shouldBe HttpStatusCode.OK
                    response.contentType() shouldBe ContentType.Application.Json.withCharset(Charsets.UTF_8)
                    val body: AccountsResponse = response.body()
                    body.items.size shouldBe 2
                    checkingAccountId = body.items.firstOrNull { it.name == "Checking" }?.id
                    checkingAccountId.shouldNotBeNull()
                }
                "type=real,type=category".asClue {
                    val response: HttpResponse =
                        client.get("$baseUrl/budgets/${budgetId}/accounts?type=real&type=category")
                    response.status shouldBe HttpStatusCode.OK
                    response.contentType() shouldBe ContentType.Application.Json.withCharset(Charsets.UTF_8)
                    val body: AccountsResponse = response.body()
                    body.items.size shouldBe 16
                }
                "type=real,category".asClue {
                    val response: HttpResponse =
                        client.get("$baseUrl/budgets/${budgetId}/accounts?type=real,category")
                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }
            "/budgets/{budgetId}/accounts/{accountId}".asClue {
                val response: HttpResponse =
                    client.get("$baseUrl/budgets/${budgetId}/accounts/$generalAccountId")
                response.status shouldBe HttpStatusCode.OK
                response.contentType() shouldBe ContentType.Application.Json.withCharset(Charsets.UTF_8)
                val body: AccountResponse = response.body()
                body.name shouldBe "General"
            }
            var transactionId: Uuid? = null
            "/budgets/{budgetId}/accounts/{accountId}/transactions".asClue {
                val response: HttpResponse =
                    client.get("$baseUrl/budgets/${budgetId}/accounts/$generalAccountId/transactions")
                response.status shouldBe HttpStatusCode.OK
                response.contentType() shouldBe ContentType.Application.Json.withCharset(Charsets.UTF_8)
                val body: AccountTransactionsResponse = response.body()
                body.items.size shouldBe 2
                transactionId = body.items.firstOrNull()?.transactionId
                transactionId.shouldNotBeNull()
            }
            "/budgets/{budgetId}/transactions/{transactionId}".asClue {
                val response: HttpResponse =
                    client.get("$baseUrl/budgets/${budgetId}/transactions/$transactionId")
                response.status shouldBe HttpStatusCode.OK
                response.contentType() shouldBe ContentType.Application.Json.withCharset(Charsets.UTF_8)
                val body: TransactionResponse = response.body()
                body.items.size shouldBe 2
            }
//            }
        }
    }
}
