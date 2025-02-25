package bps.budget

interface InitializingBudgetDao : AutoCloseable {

    /**
     * Ensures the persistent store is ready.
     *
     * The default implementation does nothing.
     */
    fun prepForFirstLoad() {}

    /**
     * Ensures that resources held by the DAO are released.
     *
     * The default implementation does nothing.
     */
    override fun close() {}

}
