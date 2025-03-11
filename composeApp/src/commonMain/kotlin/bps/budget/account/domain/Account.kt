package bps.budget.account.domain

import bps.budget.model.AccountType
import bps.kotlin.DecimalWithCents
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class Account(
    val name: String,
    val id: Uuid,
    val type: AccountType,
    val balance: DecimalWithCents,
    val description: String,
    val budgetId: Uuid,
    val companionId: Uuid? = null,
) {
    init {
        require(
            (companionId === null) == (type !== AccountType.draft),
        )
    }
}
