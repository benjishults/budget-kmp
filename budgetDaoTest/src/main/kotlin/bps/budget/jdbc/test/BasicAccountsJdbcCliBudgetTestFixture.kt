package bps.budget.jdbc.test

import bps.budget.model.AccountType
import bps.budget.model.AuthenticatedUser
import bps.budget.persistence.AccountDao
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
import java.math.BigDecimal
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
        initializeDb: () -> Unit = {},
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
        persistWithBasicAccounts(
            budgetName = budgetName,
            generalAccountId = generalAccountId,
            timeZone = timeZone,
            budgetId = budgetId,
            accountDao = accountDao,
        )
    }

    companion object {
        operator fun invoke(
            jdbcConfig: JdbcConfig,
            budgetName: String,
            userName: String,
        ): BasicAccountsJdbcCliBudgetTestFixture {
            return object : BasicAccountsJdbcCliBudgetTestFixture,
                JdbcCliBudgetTestFixture by JdbcCliBudgetTestFixture(jdbcConfig, budgetName) {
                override val userName: String = userName
            }
        }

        // TODO consider creating all these accounts on first run
        const val defaultWalletAccountName = "Wallet"
        const val defaultWalletAccountDescription = "Cash on hand"
        const val defaultCheckingAccountName = "Checking"
        const val defaultCheckingAccountDescription = "Account from which checks clear"

        const val defaultCosmeticsAccountName = "Cosmetics"
        const val defaultCosmeticsAccountDescription = "Cosmetics, procedures, pampering, and accessories"
        const val defaultEducationAccountName = "Education"
        const val defaultEducationAccountDescription = "Tuition, books, etc."
        const val defaultEntertainmentAccountName = "Entertainment"
        const val defaultEntertainmentAccountDescription = "Games, books, subscriptions, going out for food or fun"
        const val defaultFoodAccountName = "Food"
        const val defaultFoodAccountDescription = "Food other than what's covered in entertainment"
        const val defaultHobbyAccountName = "Hobby"
        const val defaultHobbyAccountDescription = "Expenses related to a hobby"
        const val defaultHomeAccountName = "Home Upkeep"
        const val defaultHomeAccountDescription =
            "Upkeep: association fees, furnace filters, appliances, repairs, lawn care"
        const val defaultHousingAccountName = "Housing"
        const val defaultHousingAccountDescription = "Rent, mortgage, property tax, insurance"
        const val defaultMedicalAccountName = "Medical"
        const val defaultMedicalAccountDescription = "Medicine, supplies, insurance, etc."
        const val defaultNecessitiesAccountName = "Necessities"
        const val defaultNecessitiesAccountDescription = "Energy, water, cleaning supplies, soap, tooth brushes, etc."
        const val defaultNetworkAccountName = "Network"
        const val defaultNetworkAccountDescription = "Mobile plan, routers, internet access"
        const val defaultTransportationAccountName = "Transportation"
        const val defaultTransportationAccountDescription = "Fares, vehicle payments, insurance, fuel, up-keep, etc."
        const val defaultTravelAccountName = "Travel"
        const val defaultTravelAccountDescription = "Travel expenses for vacation"
        const val defaultWorkAccountName = "Work"
        const val defaultWorkAccountDescription = "Work-related expenses (possibly to be reimbursed)"

        @JvmStatic
        fun persistWithBasicAccounts(
            budgetName: String,
            timeZone: TimeZone = TimeZone.Companion.currentSystemDefault(),
            checkingBalance: BigDecimal = BigDecimal.ZERO.setScale(2),
            walletBalance: BigDecimal = BigDecimal.ZERO.setScale(2),
            generalAccountId: Uuid = Uuid.random(),
            budgetId: Uuid = Uuid.random(),
            accountDao: AccountDao,
        ) {
            accountDao.createRealAndDraftAccountOrNull(
                name = defaultCheckingAccountName,
                description = defaultCheckingAccountDescription,
                balance = checkingBalance,
                budgetId = budgetId,
            )!!
            accountDao.createGeneralAccountWithIdOrNull(
                id = generalAccountId,
                balance = checkingBalance + walletBalance,
                budgetId = budgetId,
            )!!
            accountDao.createAccountOrNull(
                name = defaultWalletAccountName,
                description = defaultWalletAccountDescription,
                balance = walletBalance,
                type = AccountType.real.name,
                budgetId = budgetId,
            )!!
            accountDao.createCategoryAccountOrNull(
                name = defaultCosmeticsAccountName,
                description = defaultCosmeticsAccountDescription,
                budgetId = budgetId,
            )!!
            accountDao.createCategoryAccountOrNull(
                defaultEducationAccountName,
                defaultEducationAccountDescription,
                budgetId = budgetId,
            )!!
            accountDao.createCategoryAccountOrNull(
                defaultEntertainmentAccountName,
                defaultEntertainmentAccountDescription,
                budgetId = budgetId,
            )!!
            accountDao.createCategoryAccountOrNull(
                defaultFoodAccountName,
                defaultFoodAccountDescription,
                budgetId = budgetId,
            )!!
            accountDao.createCategoryAccountOrNull(
                defaultHobbyAccountName,
                defaultHobbyAccountDescription,
                budgetId = budgetId,
            )!!
            accountDao.createCategoryAccountOrNull(
                defaultHomeAccountName,
                defaultHomeAccountDescription,
                budgetId = budgetId,
            )!!
            accountDao.createCategoryAccountOrNull(
                defaultHousingAccountName,
                defaultHousingAccountDescription,
                budgetId = budgetId,
            )!!
            accountDao.createCategoryAccountOrNull(
                defaultMedicalAccountName,
                defaultMedicalAccountDescription,
                budgetId = budgetId,
            )!!
            accountDao.createCategoryAccountOrNull(
                defaultNecessitiesAccountName,
                defaultNecessitiesAccountDescription,
                budgetId = budgetId,
            )!!
            accountDao.createCategoryAccountOrNull(
                defaultNetworkAccountName,
                defaultNetworkAccountDescription,
                budgetId = budgetId,
            )!!
            accountDao.createCategoryAccountOrNull(
                defaultTransportationAccountName,
                defaultTransportationAccountDescription,
                budgetId = budgetId,
            )!!
            accountDao.createCategoryAccountOrNull(
                defaultTravelAccountName,
                defaultTravelAccountDescription,
                budgetId = budgetId,
            )!!
            accountDao.createCategoryAccountOrNull(
                defaultWorkAccountName,
                defaultWorkAccountDescription,
                budgetId = budgetId,
            )!!
        }
    }

}
