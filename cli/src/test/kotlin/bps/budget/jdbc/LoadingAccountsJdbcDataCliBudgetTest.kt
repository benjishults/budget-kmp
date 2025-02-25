package bps.budget.jdbc

import bps.budget.BudgetConfigurations
import bps.budget.model.AuthenticatedUser
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import java.util.UUID

class LoadingAccountsJdbcDataCliBudgetTest : FreeSpec() {

    init {
        val budgetConfigurations = BudgetConfigurations(sequenceOf("hasBasicAccountsJdbc.yml"))
        val basicAccountsJdbcCliBudgetTestFixture = BasicAccountsJdbcCliBudgetTestFixture(
            budgetConfigurations.persistence.jdbc!!,
            budgetConfigurations.budget.name,
            budgetConfigurations.user.defaultLogin!!,
        )
        val budgetId = UUID.fromString("89bc165a-ee70-43a4-b637-2774bcfc3ea4")
        val userId = UUID.fromString("f0f209c8-1b1e-43b3-8799-2dba58524d02")
        with(basicAccountsJdbcCliBudgetTestFixture) {
            createBasicAccountsBeforeSpec(
                budgetId,
                budgetConfigurations.budget.name,
                AuthenticatedUser(userId, budgetConfigurations.user.defaultLogin!!),
                TimeZone.of("America/Chicago"),
                Clock.System,
            )
            closeJdbcAfterSpec()

            "budget with basic accounts" {
                val budgetData = cliBudgetDao.load(budgetId, userId, accountDao)
                budgetData.generalAccount.id.toString() shouldBeEqual "dfa8a21c-f0ad-434d-bcb5-9e37749fa81e"
                budgetData.realAccounts shouldHaveSize 2
                budgetData.categoryAccounts shouldHaveSize 14
                budgetData.draftAccounts shouldHaveSize 1
            }
        }

    }

}
