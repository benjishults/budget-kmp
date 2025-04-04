package bps.budget.persistence

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class BudgetEntity(
    val budgetId: Uuid,
    val generalAccountName: String,
)
