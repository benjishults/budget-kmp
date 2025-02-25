package bps.budget

import bps.budget.model.BudgetData
import bps.budget.persistence.AccountDao
import java.util.UUID

interface CliBudgetDao : AutoCloseable {

    /**
     * Loads a [bps.budget.model.BudgetData] object from the persistent store.
     * @throws bps.budget.persistence.DataConfigurationException if data isn't found.
     * @throws NotImplementedError unless overridden
     */
    fun load(budgetId: UUID, userId: UUID, accountDao: AccountDao): BudgetData = TODO()

    /**
     * Ensures that resources held by the DAO are released.
     *
     * The default implementation does nothing.
     */
    override fun close() {}

}
