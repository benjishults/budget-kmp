package bps.budget.jdbc

import bps.budget.BudgetConfigurations
import bps.budget.JdbcCliBudgetDao
import bps.budget.JdbcInitializingBudgetDao
import bps.budget.consistency.commitTransactionConsistently
import bps.budget.jdbc.test.BasicAccountsJdbcCliBudgetTestFixture
import bps.budget.loadBudgetData
import bps.budget.model.AuthenticatedUser
import bps.budget.model.BudgetData
import bps.budget.model.CategoryAccount
import bps.budget.model.DraftAccount
import bps.budget.model.RealAccount
import bps.budget.model.Transaction
import bps.budget.model.TransactionType
import bps.budget.model.defaultCheckingAccountName
import bps.budget.model.defaultCosmeticsAccountName
import bps.budget.model.defaultEducationAccountName
import bps.budget.model.defaultEntertainmentAccountName
import bps.budget.model.defaultFoodAccountName
import bps.budget.model.defaultGeneralAccountName
import bps.budget.model.defaultHobbyAccountName
import bps.budget.model.defaultHomeAccountName
import bps.budget.model.defaultHousingAccountName
import bps.budget.model.defaultMedicalAccountName
import bps.budget.model.defaultNecessitiesAccountName
import bps.budget.model.defaultNetworkAccountName
import bps.budget.model.defaultTransportationAccountName
import bps.budget.model.defaultTravelAccountName
import bps.budget.model.defaultWalletAccountName
import bps.budget.model.defaultWorkAccountName
import bps.kotlin.test.WithMockClock
import io.kotest.assertions.fail
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SomeBasicTransactionsCliBudgetTest : FreeSpec(),
    WithMockClock {

    init {
        val budgetConfigurations = BudgetConfigurations(sequenceOf("hasBasicAccountsJdbc.yml"))
        val basicAccountsJdbcCliBudgetTestFixture = BasicAccountsJdbcCliBudgetTestFixture(
            budgetConfigurations.persistence.jdbc!!,
            budgetConfigurations.budget.name,
            budgetConfigurations.user.defaultLogin,
        )
        val clock = produceSecondTickingClock()
        val budgetId: Uuid = Uuid.parse("89bc165a-ee70-43a4-b637-2774bcfc3ea4")
        val userId: Uuid = Uuid.parse("f0f209c8-1b1e-43b3-8799-2dba58524d02")
        with(basicAccountsJdbcCliBudgetTestFixture) {
            val initializingBudgetDao = JdbcInitializingBudgetDao(budgetName, dataSource)
            val cliBudgetDao = JdbcCliBudgetDao(userName, dataSource)
            createBasicAccountsBeforeSpec(
                budgetId,
                budgetConfigurations.budget.name,
                AuthenticatedUser(userId, budgetConfigurations.user.defaultLogin),
                TimeZone.of("America/Chicago"),
                clock,
            ) {
                initializingBudgetDao.prepForFirstLoad()
            }

            "with data from config" - {
                val budgetData = loadBudgetData(
                    authenticatedUser = userBudgetDao.getUserByLoginOrNull(budgetConfigurations.user.defaultLogin) as AuthenticatedUser,
                    initializingBudgetDao = initializingBudgetDao,
                    budgetName = budgetConfigurations.budget.name,
                    cliBudgetDao = cliBudgetDao,
                    accountDao = accountDao,
                )
                "record income" {
                    val amount = BigDecimal("1000.00").setScale(2)
                    val income: Transaction =
                        Transaction
                            .Builder(
                                description = "income into $defaultCheckingAccountName",
                                timestamp = clock.now(),
                                transactionType = TransactionType.income.name,
                            )
                            .apply {
                                with(budgetData.generalAccount) {
                                    addItemBuilderTo(amount)
                                }
                                with(
                                    budgetData.realAccounts
                                        .find {
                                            it.name == defaultCheckingAccountName
                                        }!!,
                                ) {
                                    addItemBuilderTo(amount)
                                }
                            }
                            .build()
                    commitTransactionConsistently(income, transactionDao, accountDao, budgetData)
                }
                "allocate to food" {
                    val amount = BigDecimal("300.00")
                    val allocate: Transaction =
                        Transaction
                            .Builder(
                                description = "allocate into $defaultFoodAccountName",
                                timestamp = clock.now(),
                                transactionType = TransactionType.allowance.name,
                            )
                            .apply {
                                with(budgetData.generalAccount) {
                                    addItemBuilderTo(-amount)
                                }
                                with(
                                    budgetData.categoryAccounts.find {
                                        it.name == defaultFoodAccountName
                                    }!!,
                                ) {
                                    addItemBuilderTo(amount)
                                }
                            }
                            .build()
                    commitTransactionConsistently(allocate, transactionDao, accountDao, budgetData)
                }
                "write a check for food" {
                    val amount = BigDecimal("100.00")
                    val writeCheck: Transaction = Transaction.Builder(
                        description = "groceries",
                        timestamp = clock.now(),
                        transactionType = TransactionType.expense.name,
                    )
                        .apply {
                            with(
                                budgetData.categoryAccounts.find {
                                    it.name == defaultFoodAccountName
                                }!!,
                            ) {
                                addItemBuilderTo(-amount)
                            }
                            with(
                                budgetData.draftAccounts.find {
                                    it.name == defaultCheckingAccountName
                                }!!,
                            ) {
                                addItemBuilderTo(amount)
                            }
                        }
                        .build()
                    commitTransactionConsistently(writeCheck, transactionDao, accountDao, budgetData)
                }
                "check balances after writing check" {
                    budgetData.realAccounts.forEach { realAccount: RealAccount ->
                        when (realAccount.name) {
                            defaultCheckingAccountName -> {
                                realAccount.balance shouldBe BigDecimal("1000.00")
                            }
                            defaultWalletAccountName ->
                                realAccount.balance shouldBe BigDecimal.ZERO.setScale(2)
                            else ->
                                fail("unexpected real account")
                        }
                    }
                    budgetData.categoryAccounts.forEach { it: CategoryAccount ->
                        when (it.name) {
                            defaultGeneralAccountName -> it.balance shouldBe BigDecimal("700.00")
                            defaultCosmeticsAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                            defaultEducationAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                            defaultEntertainmentAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                            defaultFoodAccountName -> it.balance shouldBe BigDecimal("200.00")
                            defaultHobbyAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                            defaultHomeAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                            defaultHousingAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                            defaultMedicalAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                            defaultNecessitiesAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                            defaultNetworkAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                            defaultTransportationAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                            defaultTravelAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                            defaultWorkAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                            else -> fail("unexpected category account: $it")
                        }
                    }
                    budgetData.draftAccounts.forEach { it: DraftAccount ->
                        when (it.name) {
                            defaultCheckingAccountName -> it.balance shouldBe BigDecimal("100.00")
                            else -> fail("unexpected draft account: $it")
                        }
                    }
                }
                "check clears" {
                    val amount = BigDecimal("100.00")
                    val clearCheck: Transaction = Transaction.Builder(
                        description = "groceries",
                        timestamp = clock.now(),
                        transactionType = TransactionType.clearing.name,
                    )
                        .apply {
                            with(
                                budgetData.realAccounts.find {
                                    it.name == defaultCheckingAccountName
                                }!!,
                            ) {
                                addItemBuilderTo(-amount)
                            }
                            with(
                                budgetData.draftAccounts.find {
                                    it.name == defaultCheckingAccountName
                                }!!,
                            ) {
                                addItemBuilderTo(-amount)
                            }
                        }
                        .build()
                    commitTransactionConsistently(clearCheck, transactionDao, accountDao, budgetData)
                }
                "check balances after check clears" {
                    checkBalancesAfterCheckClears(budgetData)
                }
                "check balances in DB" {
                    checkBalancesAfterCheckClears(cliBudgetDao.load(budgetData.id, userId, accountDao))
                }
            }
        }

    }

    private fun checkBalancesAfterCheckClears(budgetData: BudgetData) {
        budgetData.realAccounts.size shouldBe 2
        budgetData.realAccounts.forEach { realAccount: RealAccount ->
            when (realAccount.name) {
                defaultCheckingAccountName -> {
                    realAccount.balance shouldBe BigDecimal("900.00")
                }
                defaultWalletAccountName ->
                    realAccount.balance shouldBe BigDecimal.ZERO.setScale(2)
                else ->
                    fail("unexpected real account: $realAccount")
            }
        }
        budgetData.categoryAccounts.size shouldBe 14
        budgetData.categoryAccounts.forEach { it: CategoryAccount ->
            when (it.name) {
                defaultGeneralAccountName -> it.balance shouldBe BigDecimal("700.00")
                defaultCosmeticsAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                defaultFoodAccountName -> it.balance shouldBe BigDecimal("200.00")
                defaultHobbyAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                defaultHomeAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                defaultHousingAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                defaultNecessitiesAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                defaultWorkAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                defaultTransportationAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                defaultTravelAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                defaultMedicalAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                defaultEducationAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                defaultEntertainmentAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                defaultNetworkAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                else -> fail("unexpected category account: $it")
            }
        }
        budgetData.draftAccounts.size shouldBe 1
        budgetData.draftAccounts.forEach { it: DraftAccount ->
            when (it.name) {
                defaultCheckingAccountName -> it.balance shouldBe BigDecimal.ZERO.setScale(2)
                else -> fail("unexpected draft account: $it")
            }
        }
    }

}
