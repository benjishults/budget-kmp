package bps.budget.jdbc.test

import bps.budget.model.AuthenticatedUser
import bps.budget.model.BudgetData
import bps.jdbc.JdbcConfig
import bps.time.atStartOfMonth
import io.kotest.core.spec.Spec
import io.kotest.mpp.atomics.AtomicReference
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface BasicAccountsJdbcCliBudgetTestFixture : JdbcCliBudgetTestFixture {

    val userName: String

    /**
     * Ensure that basic accounts are in place with zero balances in the DB before the test starts and deletes
     * transactions once the test is done.
     */
    fun Spec.createBasicAccountsBeforeSpec(
        budgetId: Uuid,
        budgetName: String,
        authenticatedUser: AuthenticatedUser,
        timeZone: TimeZone,
        clock: Clock,
        initializeDb: () -> Unit = {}
    ) {
        beforeSpec {
            initializeDb()
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

    fun Spec.resetAfterEach(budgetId: AtomicReference<Uuid?>) {
        afterEach {
            cleanupTransactions(budgetId.value!!, jdbcConnectionProvider.connection)
        }
    }

    fun Spec.resetBalancesAndTransactionAfterSpec(budgetId: Uuid) {
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
        generalAccountId: Uuid = Uuid.parse("dfa8a21c-f0ad-434d-bcb5-9e37749fa81e"),
        timeZone: TimeZone = TimeZone.Companion.UTC,
        authenticatedUser: AuthenticatedUser,
        budgetId: Uuid,
        clock: Clock,
    ) {
//        initializingBudgetDao.prepForFirstLoad()
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
        BudgetData.Companion.persistWithBasicAccounts(
            budgetName = budgetName,
            generalAccountId = generalAccountId,
            timeZone = timeZone,
            budgetId = budgetId,
            accountDao = accountDao,
        )
    }

    companion object {
        operator fun invoke(jdbcConfig: JdbcConfig, budgetName: String, userName: String): BasicAccountsJdbcCliBudgetTestFixture {
            return object : BasicAccountsJdbcCliBudgetTestFixture,
                JdbcCliBudgetTestFixture by JdbcCliBudgetTestFixture(jdbcConfig, budgetName) {
                override val userName: String = userName
            }
        }
    }

}
