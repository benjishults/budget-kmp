package bps.budget

import bps.budget.model.AuthenticatedUser
import bps.budget.model.BudgetData
import bps.budget.BudgetDao
import bps.budget.ui.UiFacade
import bps.console.app.MenuApplicationWithQuit
import bps.console.io.DefaultInputReader
import bps.console.io.DefaultOutPrinter
import bps.console.io.InputReader
import bps.console.io.OutPrinter
import bps.console.io.WithIo
import kotlinx.datetime.Clock

const val recordIncomeLabel = "Record Income"
const val makeAllowancesLabel = "Make Allowances"
const val writeOrClearChecksLabel = "Write or Clear Checks"
const val useOrPayCreditCardsLabel = "Use or Pay Credit Cards"
const val transferLabel = "Transfer Money"
const val manageAccountsLabel = "Manage Accounts"
const val recordSpendingLabel = "Record Spending"
const val manageTransactionsLabel = "Manage Transactions"
const val userSettingsLabel = "User Settings"

class BudgetApplication private constructor(
    override val inputReader: InputReader,
    override val outPrinter: OutPrinter,
    uiFacade: UiFacade,
    val budgetDao: BudgetDao,
    clock: Clock,
    configurations: BudgetConfigurations,
) : AutoCloseable, WithIo {

    constructor(
        uiFacade: UiFacade,
        configurations: BudgetConfigurations,
        inputReader: InputReader = DefaultInputReader,
        outPrinter: OutPrinter = DefaultOutPrinter,
        clock: Clock = Clock.System,
    ) : this(
        inputReader,
        outPrinter,
        uiFacade,
        buildBudgetDao(configurations.persistence),
        clock,
        configurations,
    )

    init {
        budgetDao.prepForFirstLoad()
    }

    val authenticatedUser: AuthenticatedUser = uiFacade.login(budgetDao.userBudgetDao, configurations.user)
    val budgetData: BudgetData = loadOrBuildBudgetData(
        authenticatedUser = authenticatedUser,
        uiFacade = uiFacade,
        budgetDao = budgetDao,
        budgetName = getBudgetNameFromPersistenceConfig(configurations.persistence) ?: uiFacade.getBudgetName(),
        clock = clock,
    )

    val menuApplicationWithQuit =
        MenuApplicationWithQuit(
            budgetMenu(
                budgetData,
                budgetDao,
                configurations.user,
                authenticatedUser.id,
                clock,
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
        budgetDao.close()
        menuApplicationWithQuit.close()
    }

}
