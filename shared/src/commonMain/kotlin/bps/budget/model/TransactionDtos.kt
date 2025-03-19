@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.model

import bps.kotlin.DecimalWithCents
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
class TransactionsResponse(
    val items: List<TransactionResponse>,
    val links: Links? = null,
)

@Serializable
class TransactionResponse(
    val id: Uuid,
    val type: TransactionType,
    val description: String? = null,
    val timestamp: Instant,
    val items: List<TransactionItemResponse>,
    val clearsId: Uuid? = null,
    val budgetId: Uuid,
)

// NOTE Should this have the transactionId as well?
//      1. CON: it should always and only be returned in the context of the items list of a transaction
//      2. PRO: it might be convenient for the client to have this id?
//      I'm going to say 'no' for now.
@Serializable
class TransactionItemResponse(
    val id: Uuid,
    val description: String? = null,
    val amount: DecimalWithCents,
    val draftStatus: DraftStatus,
    val accountId: Uuid,
    val budgetId: Uuid,
)

@Serializable
class AccountTransactionsResponse(
    val items: List<AccountTransactionResponse>,
    val links: Links? = null,
)

@Serializable
class AccountTransactionResponse(
    val transactionItemId: Uuid,
    val transactionId: Uuid,
    val timestamp: Instant,
    val description: String? = null,
    val amount: DecimalWithCents,
    val balance: DecimalWithCents? = null,
    val type: TransactionType? = null,
    val accountId: Uuid,
    val budgetId: Uuid,
)
