package bps.budget.model

enum class TransactionType {
    expense,
    income,

    /**
     * Income or expenditure that isn't included in income or expenditure calculations.
     */
    reimburse,

    /**
     * Starting to track an existing real account for the first time.
     */
    initial,

    /**
     * transfer from General to a category
     */
    allowance,
    transfer,

    /**
     * transfer from real to charge or draft
     */
    clearing,
}
