package bps.budget.jdbc

import bps.budget.model.AuthenticatedUser
import bps.budget.model.BudgetData
import bps.time.atStartOfMonth
import io.kotest.core.spec.Spec
import io.kotest.mpp.atomics.AtomicReference
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

interface BasicAccountsJdbcCliBudgetTestFixture : JdbcCliBudgetTestFixture {

    /**
     * Ensure that basic accounts are in place with zero balances in the DB before the test starts and deletes
     * transactions once the test is done.
     */
    fun Spec.createBasicAccountsBeforeSpec(
        budgetId: UUID,
        budgetName: String,
        authenticatedUser: AuthenticatedUser,
        timeZone: TimeZone,
        clock: Clock,
    ) {
        beforeSpec {
            initializingBudgetDao.prepForFirstLoad()
//            try {
            deleteAccounts(budgetId, jdbcConnectionProvider.connection)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//            try {
            userBudgetDao.deleteBudget(budgetId)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//            try {
            userBudgetDao.deleteUser(authenticatedUser.id)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//            try {
            userBudgetDao.deleteUserByLogin(authenticatedUser.login)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
            upsertBasicAccounts(
                budgetName,
                timeZone = timeZone,
                authenticatedUser = authenticatedUser,
                budgetId = budgetId,
                clock = clock,
            )
        }
    }

    fun Spec.resetAfterEach(budgetId: AtomicReference<UUID?>) {
        afterEach {
            cleanupTransactions(budgetId.value!!, jdbcConnectionProvider.connection)
        }
    }

    fun Spec.resetBalancesAndTransactionAfterSpec(budgetId: UUID) {
        afterSpec {
            cleanupTransactions(budgetId, jdbcConnectionProvider.connection)
        }
    }

    /**
     * This will be called automatically before a spec starts if you've called [createBasicAccountsBeforeSpec].
     * This ensures the DB contains the basic accounts with zero balances.
     */
    private fun upsertBasicAccounts(
        budgetName: String,
        generalAccountId: UUID = UUID.fromString("dfa8a21c-f0ad-434d-bcb5-9e37749fa81e"),
        timeZone: TimeZone = TimeZone.UTC,
        authenticatedUser: AuthenticatedUser,
        budgetId: UUID,
        clock: Clock,
    ) {
        initializingBudgetDao.prepForFirstLoad()
        userBudgetDao.createUser(authenticatedUser.login, "a", authenticatedUser.id)
        userBudgetDao.createBudgetOrNull(generalAccountId, budgetId)!!
        userBudgetDao.grantAccess(
            budgetName = budgetName,
            timeZoneId = timeZone.id,
            analyticsStart =
                clock
                    .now()
                    .toLocalDateTime(timeZone)
                    .let { now ->
                        if (now.month == Month.DECEMBER) {
                            LocalDateTime(now.year + 1, 1, 1, 0, 0, 0)
                        } else {
                            LocalDateTime(now.year, now.month + 1, 1, 0, 0, 0)
                        }
                    }
                    .atStartOfMonth()
                    .toInstant(timeZone),
            userId = authenticatedUser.id,
            budgetId = budgetId,
        )
        BudgetData.persistWithBasicAccounts(
            budgetName = budgetName,
            generalAccountId = generalAccountId,
            timeZone = timeZone,
            budgetId = budgetId,
            accountDao = accountDao,
        )
    }

    companion object {
        operator fun invoke(configFileName: String = "hasBasicAccountsJdbc.yml"): BasicAccountsJdbcCliBudgetTestFixture =
            object : BasicAccountsJdbcCliBudgetTestFixture,
                JdbcCliBudgetTestFixture by JdbcCliBudgetTestFixture(configFileName) {}
    }

}
