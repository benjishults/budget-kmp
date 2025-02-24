package bps.budget.jdbc

import bps.budget.JdbcDao
import bps.budget.model.AuthenticatedUser
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import java.util.UUID

class LoadingAccountsJdbcDataTest : FreeSpec(), BasicAccountsJdbcTestFixture {

    override val jdbcDao = JdbcDao(configurations.persistence.jdbc!!, configurations.budget.name)

    init {
        val budgetId = UUID.fromString("89bc165a-ee70-43a4-b637-2774bcfc3ea4")
        val userId = UUID.fromString("f0f209c8-1b1e-43b3-8799-2dba58524d02")
        createBasicAccountsBeforeSpec(
            budgetId,
            configurations.budget.name,
            AuthenticatedUser(userId, configurations.user.defaultLogin!!),
            TimeZone.of("America/Chicago"),
            Clock.System,
        )
        closeJdbcAfterSpec()

        "budget with basic accounts" {
            val budgetData = jdbcDao.load(budgetId, userId)
            budgetData.generalAccount.id.toString() shouldBeEqual "dfa8a21c-f0ad-434d-bcb5-9e37749fa81e"
            budgetData.realAccounts shouldHaveSize 2
            budgetData.categoryAccounts shouldHaveSize 14
            budgetData.draftAccounts shouldHaveSize 1
        }

    }

}
