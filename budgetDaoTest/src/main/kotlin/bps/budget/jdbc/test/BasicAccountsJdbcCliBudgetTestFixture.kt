package bps.budget.jdbc.test

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
     * @return the budgetId
     */
    fun Spec.createBasicAccountsBeforeSpec(
        budgetName: String,
        userName: String,
        timeZone: TimeZone,
        clock: Clock,
        initializeDb: () -> Unit = {},
    ) {
        beforeSpec {
            initializeDb()
            initializeWithBasicAccounts(
                budgetName,
                userName,
                timeZone = timeZone,
                clock = clock,
            )
        }
    }

    fun Spec.cleanUpTransactionsAfterEach(budgetId: AtomicReference<Uuid?>) {
        afterEach {
            deleteTransactions(budgetId.value!!, dataSource)
        }
    }

//    fun Spec.resetBalancesAndTransactionAfterSpec(budgetId: Uuid) {
//        afterSpec {
//            cleanupTransactions(budgetId, dataSource)
//        }
//    }

    fun Spec.cleanUpEverythingAfterSpec(userName: String) {
        afterSpec {
            deleteUser(userName, dataSource)
        }
    }

    /**
     * This will be called automatically before a spec starts if you've called [createBasicAccountsBeforeSpec].
     * This ensures the DB contains the basic accounts with zero balances.
     * @return the budgetId
     */
    private fun initializeWithBasicAccounts(
        budgetName: String,
        userName: String,
        timeZone: TimeZone = TimeZone.Companion.UTC,
        clock: Clock,
    ): Uuid {
        val userId = userBudgetDao.createUser(userName, "a").userId
        val budgetId: Uuid =
            userBudgetDao
                .createBudget()
                .budgetId
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
            userId = userId,
            budgetId = budgetId,
        )
        persistWithBasicAccounts(
            budgetName = budgetName,
            timeZone = timeZone,
            budgetId = budgetId,
            accountDao = accountDao,
        )
        return budgetId
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
            accountDao.createRealAndDraftAccount(
                name = defaultCheckingAccountName,
                description = defaultCheckingAccountDescription,
                balance = checkingBalance,
                budgetId = budgetId,
            )
            accountDao.createGeneralAccountWithId(
                id = generalAccountId,
                balance = checkingBalance + walletBalance,
                budgetId = budgetId,
            )
            accountDao.createRealAccount(
                name = defaultWalletAccountName,
                description = defaultWalletAccountDescription,
                balance = walletBalance,
                budgetId = budgetId,
            )
            accountDao.createCategoryAccount(
                name = defaultCosmeticsAccountName,
                description = defaultCosmeticsAccountDescription,
                budgetId = budgetId,
            )
            accountDao.createCategoryAccount(
                defaultEducationAccountName,
                defaultEducationAccountDescription,
                budgetId = budgetId,
            )
            accountDao.createCategoryAccount(
                defaultEntertainmentAccountName,
                defaultEntertainmentAccountDescription,
                budgetId = budgetId,
            )
            accountDao.createCategoryAccount(
                defaultFoodAccountName,
                defaultFoodAccountDescription,
                budgetId = budgetId,
            )
            accountDao.createCategoryAccount(
                defaultHobbyAccountName,
                defaultHobbyAccountDescription,
                budgetId = budgetId,
            )
            accountDao.createCategoryAccount(
                defaultHomeAccountName,
                defaultHomeAccountDescription,
                budgetId = budgetId,
            )
            accountDao.createCategoryAccount(
                defaultHousingAccountName,
                defaultHousingAccountDescription,
                budgetId = budgetId,
            )
            accountDao.createCategoryAccount(
                defaultMedicalAccountName,
                defaultMedicalAccountDescription,
                budgetId = budgetId,
            )
            accountDao.createCategoryAccount(
                defaultNecessitiesAccountName,
                defaultNecessitiesAccountDescription,
                budgetId = budgetId,
            )
            accountDao.createCategoryAccount(
                defaultNetworkAccountName,
                defaultNetworkAccountDescription,
                budgetId = budgetId,
            )
            accountDao.createCategoryAccount(
                defaultTransportationAccountName,
                defaultTransportationAccountDescription,
                budgetId = budgetId,
            )
            accountDao.createCategoryAccount(
                defaultTravelAccountName,
                defaultTravelAccountDescription,
                budgetId = budgetId,
            )
            accountDao.createCategoryAccount(
                defaultWorkAccountName,
                defaultWorkAccountDescription,
                budgetId = budgetId,
            )
        }
    }

}
