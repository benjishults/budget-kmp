package bps.budget

import bps.budget.model.BudgetData
import bps.budget.persistence.AccountDao
import bps.budget.persistence.AnalyticsDao
import bps.budget.persistence.TransactionDao
import bps.budget.persistence.UserBudgetDao
import java.util.UUID

interface BudgetDao : AutoCloseable {

    val userBudgetDao: UserBudgetDao get() = TODO()
    val transactionDao: TransactionDao get() = TODO()
    val accountDao: AccountDao get() = TODO()
    val analyticsDao: AnalyticsDao get() = TODO()

    /**
     * Ensures the persistent store is ready.
     *
     * The default implementation does nothing.
     */
    fun prepForFirstLoad() {}

    /**
     * Loads a [bps.budget.model.BudgetData] object from the persistent store.
     * @throws bps.budget.persistence.DataConfigurationException if data isn't found.
     * @throws NotImplementedError unless overridden
     */
    fun load(budgetId: UUID, userId: UUID): BudgetData = TODO()

    /**
     * Ensures that resources held by the DAO are released.
     *
     * This implementation does nothing.
     */
    override fun close() {}

}
