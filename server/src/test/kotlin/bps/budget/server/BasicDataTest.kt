package bps.budget.server

import bps.budget.model.AccountResponse
import bps.budget.model.AccountTransactionsResponse
import bps.budget.model.AccountType
import bps.budget.model.AccountsResponse
import bps.budget.model.TransactionResponse
import bps.budget.persistence.jdbc.JdbcAccountDao
import bps.budget.persistence.jdbc.JdbcTransactionDao
import bps.jdbc.configureDataSource
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
class BasicDataTest : FreeSpec() {

    init {
        val budgetServerConfigurations = BudgetServerConfigurations(sequenceOf("budget-server.yml"))
        val dataSource = configureDataSource(budgetServerConfigurations.jdbc, budgetServerConfigurations.hikari)
        val accountDao = JdbcAccountDao(dataSource)
        val transactionDao = JdbcTransactionDao(dataSource)
        val budgetId: Uuid = Uuid.parse("89bc165a-ee70-43a4-b637-2774bcfc3ea4")
//        val userId = Uuid.parse("f0f209c8-1b1e-43b3-8799-2dba58524d02")
//        val baseUrl = "http://localhost:8085"
        val baseUrl = ""
        testApplication {
            application {
                module(accountDao, transactionDao)
            }
            val client = HttpClient {
                this.install(ContentNegotiation) { json() }
            }
            "/" {
                val response: HttpResponse = client.get("$baseUrl/")
                response.status shouldBe HttpStatusCode.OK
                response.contentType() shouldBe ContentType.Text.Plain.withCharset(Charsets.UTF_8)
                response.bodyAsText() shouldBe "root"
            }
            "/content/sample.html" {
                val response: HttpResponse = client.get("$baseUrl/content/sample.html")
                response.status shouldBe HttpStatusCode.OK
                response.contentType() shouldBe ContentType.Text.Html.withCharset(Charsets.UTF_8)
                response.bodyAsText() shouldContain "li>Ktor</li"
            }
            "/test" {
                val response: HttpResponse = client.get("$baseUrl/test")
                response.status shouldBe HttpStatusCode.OK
                response.contentType() shouldBe ContentType.Text.Plain.withCharset(Charsets.UTF_8)
                response.bodyAsText() shouldBe "succeeded"
            }
            var generalAccountId: Uuid? = null
            var checkingAccountId: Uuid? = null
            var draftAccountId: Uuid? = null
            var foodAccountId: Uuid? = null
            "/budgets/{budgetId}/accounts" - {
                "no type specified" {
                    val response: HttpResponse = client.get("$baseUrl/budgets/${budgetId}/accounts")
                    response.status shouldBe HttpStatusCode.OK
                    response.contentType() shouldBe ContentType.Application.Json.withCharset(Charsets.UTF_8)
                    val body: AccountsResponse = response.body()
                    body.items.size shouldBe 20
                    draftAccountId = body.items.firstOrNull { it.type == AccountType.draft.name }?.id
                    draftAccountId.shouldNotBeNull()
                }
                "type=category" {
                    val response: HttpResponse = client.get("$baseUrl/budgets/${budgetId}/accounts?type=category")
                    response.status shouldBe HttpStatusCode.OK
                    response.contentType() shouldBe ContentType.Application.Json.withCharset(Charsets.UTF_8)
                    val body: AccountsResponse = response.body()
                    body.items.size shouldBe 15
                    generalAccountId = body.items.firstOrNull { it.name == "General" }?.id
                    generalAccountId.shouldNotBeNull()
                    foodAccountId = body.items.firstOrNull { it.name == "Food" }?.id
                    foodAccountId.shouldNotBeNull()
                }
                "type=real" {
                    val response: HttpResponse = client.get("$baseUrl/budgets/${budgetId}/accounts?type=real")
                    response.status shouldBe HttpStatusCode.OK
                    response.contentType() shouldBe ContentType.Application.Json.withCharset(Charsets.UTF_8)
                    val body: AccountsResponse = response.body()
                    body.items.size shouldBe 2
                    checkingAccountId = body.items.firstOrNull { it.name == "Checking" }?.id
                    checkingAccountId.shouldNotBeNull()
                }
                "type=real,type=category" {
                    val response: HttpResponse =
                        client.get("$baseUrl/budgets/${budgetId}/accounts?type=real&type=category")
                    response.status shouldBe HttpStatusCode.OK
                    response.contentType() shouldBe ContentType.Application.Json.withCharset(Charsets.UTF_8)
                    val body: AccountsResponse = response.body()
                    body.items.size shouldBe 18
                }
                "type=real,category" {
                    val response: HttpResponse = client.get("$baseUrl/budgets/${budgetId}/accounts?type=real,category")
                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }
            "/budgets/{budgetId}/accounts/{accountId}" {
                val response: HttpResponse = client.get("$baseUrl/budgets/${budgetId}/accounts/$generalAccountId")
                response.status shouldBe HttpStatusCode.OK
                response.contentType() shouldBe ContentType.Application.Json.withCharset(Charsets.UTF_8)
                val body: AccountResponse = response.body()
                body.name shouldBe "General"
            }
            var transactionId: Uuid? = null
            "/budgets/{budgetId}/accounts/{accountId}/transactions" {
                val response: HttpResponse =
                    client.get("$baseUrl/budgets/${budgetId}/accounts/$generalAccountId/transactions")
                response.status shouldBe HttpStatusCode.OK
                response.contentType() shouldBe ContentType.Application.Json.withCharset(Charsets.UTF_8)
                val body: AccountTransactionsResponse = response.body()
                body.items.size shouldBe 1
                transactionId = body.items.firstOrNull()?.transactionId
                transactionId.shouldNotBeNull()
            }
            "/budgets/{budgetId}/transactions/{transactionId}" {
                val response: HttpResponse = client.get("$baseUrl/budgets/${budgetId}/transactions/$transactionId")
                response.status shouldBe HttpStatusCode.OK
                response.contentType() shouldBe ContentType.Application.Json.withCharset(Charsets.UTF_8)
                val body: TransactionResponse = response.body()
                body.items.size shouldBe 2
            }
        }
    }
}
