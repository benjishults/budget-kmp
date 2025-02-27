package bps.budget.server

import bps.budget.persistence.jdbc.JdbcAccountDao
import bps.jdbc.toJdbcConnectionProvider
import io.kotest.assertions.asClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class TestTest : FreeSpec() {

    init {
        val budgetServerConfigurations = BudgetServerConfigurations(sequenceOf("budget-server.yml"))
//        val basicAccountsJdbcCliBudgetTestFixture = BasicAccountsJdbcCliBudgetTestFixture(
//            budgetServerConfigurations.jdbc,
//            "Basic Accounts Test",
//            "test@test.com",
//        )
        val jdbcConnectionProvider = budgetServerConfigurations.jdbc.toJdbcConnectionProvider()
        val accountDao = JdbcAccountDao(jdbcConnectionProvider)
        val budgetId: Uuid = Uuid.parse("89bc165a-ee70-43a4-b637-2774bcfc3ea4")
        val userId = Uuid.parse("f0f209c8-1b1e-43b3-8799-2dba58524d02")
        testApplication {
            application {
                module(accountDao)
            }
            client.get("/budgets/$budgetId/accounts") {
                header("Content-Type", "application/json")
            }
                .asClue {
                    it.status shouldBe HttpStatusCode.OK
                }
        }
    }
}
