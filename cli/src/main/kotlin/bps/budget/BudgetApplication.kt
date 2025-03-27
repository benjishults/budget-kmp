package bps.budget

import bps.budget.model.AuthenticatedUser
import bps.budget.model.BudgetData
import bps.budget.persistence.AccountDao
import bps.budget.persistence.AnalyticsDao
import bps.budget.persistence.TransactionDao
import bps.budget.persistence.UserBudgetDao
import bps.budget.persistence.jdbc.JdbcAccountDao
import bps.budget.persistence.jdbc.JdbcAnalyticsDao
import bps.budget.persistence.jdbc.JdbcTransactionDao
import bps.budget.persistence.jdbc.JdbcUserBudgetDao
import bps.budget.ui.UiFacade
import bps.console.app.MenuApplicationWithQuit
import bps.console.io.DefaultInputReader
import bps.console.io.DefaultOutPrinter
import bps.console.io.InputReader
import bps.console.io.OutPrinter
import bps.console.io.WithIo
import bps.jdbc.configureDataSource
import kotlinx.datetime.Clock
import javax.sql.DataSource
import kotlin.uuid.ExperimentalUuidApi

const val recordIncomeLabel = "Record Income"
const val makeAllowancesLabel = "Make Allowances"
const val writeOrClearChecksLabel = "Write or Clear Checks"
const val useOrPayCreditCardsLabel = "Use or Pay Credit Cards"
const val transferLabel = "Transfer Money"
const val manageAccountsLabel = "Manage Accounts"
const val recordSpendingLabel = "Record Spending"
const val manageTransactionsLabel = "Manage Transactions"
const val userSettingsLabel = "User Settings"

@OptIn(ExperimentalUuidApi::class)
class BudgetApplication private constructor(
    override val inputReader: InputReader,
    override val outPrinter: OutPrinter,
    uiFacade: UiFacade,
    val initializingBudgetDao: InitializingBudgetDao,
    val cliBudgetDao: CliBudgetDao,
    val accountDao: AccountDao,
    val transactionDao: TransactionDao,
    val analyticsDao: AnalyticsDao,
    val userBudgetDao: UserBudgetDao,
    clock: Clock,
    configurations: BudgetConfigurations,
) : AutoCloseable, WithIo {

    constructor(
        uiFacade: UiFacade,
        configurations: BudgetConfigurations,
        inputReader: InputReader = DefaultInputReader,
        outPrinter: OutPrinter = DefaultOutPrinter,
        clock: Clock = Clock.System,
        dataSource: DataSource = configureDataSource(configurations.persistence.jdbc!!, configurations.hikari),
    ) : this(
        uiFacade = uiFacade,
        outPrinter = outPrinter,
        inputReader = inputReader,
        initializingBudgetDao = JdbcInitializingBudgetDao(
            configurations.budget.name,
            dataSource,
        ),
        cliBudgetDao = JdbcCliBudgetDao(configurations.budget.name, dataSource),
        accountDao = JdbcAccountDao(dataSource),
        transactionDao = JdbcTransactionDao(dataSource),
        analyticsDao = JdbcAnalyticsDao(dataSource),
        userBudgetDao = JdbcUserBudgetDao(dataSource),
        clock = clock,
        configurations = configurations,
    )

    init {
        initializingBudgetDao.prepForFirstLoad()
    }

    val authenticatedUser: AuthenticatedUser = uiFacade.login(userBudgetDao, configurations.user)
    val budgetData: BudgetData = loadOrBuildBudgetData(
        authenticatedUser = authenticatedUser,
        uiFacade = uiFacade,
        initializingBudgetDao = initializingBudgetDao,
        cliBudgetDao = cliBudgetDao,
        accountDao = accountDao,
        budgetName = configurations.budget.name,
        clock = clock,
        userBudgetDao = userBudgetDao,
    )

    val menuApplicationWithQuit =
        MenuApplicationWithQuit(
            budgetMenu(
                budgetData = budgetData,
                accountDao = accountDao,
                transactionDao = transactionDao,
                analyticsDao = analyticsDao,
                userBudgetDao = userBudgetDao,
                userConfig = configurations.user,
                userId = authenticatedUser.id,
                clock = clock,
            ),
            inputReader,
            outPrinter,
        )

    fun run() {
        menuApplicationWithQuit.runApplication()
    }

    override fun close() {
        if (!budgetData.validate())
            outPrinter.important("Budget Data was invalid on exit!")
        initializingBudgetDao.close()
        menuApplicationWithQuit.close()
    }

}
