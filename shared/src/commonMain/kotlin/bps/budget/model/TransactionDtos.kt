@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.model

import bps.kotlin.DecimalWithCents
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
class AccountTransactionResponse(
    val id: Uuid,
    val type: TransactionType? = null,
    val balance: DecimalWithCents? = null,
    val amount: DecimalWithCents,
    val description: String? = null,
    val budgetId: Uuid,
    val accountId: Uuid,
)
