@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.persistence

import bps.budget.model.AccountData
import bps.budget.model.AccountType
import kotlinx.datetime.Instant
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// TODO I doubt this is used.  Consider removing.
interface TransactionData : Comparable<TransactionData> {
    val id: Uuid
    val description: String
    val timestamp: Instant
    val transactionType: String
}

/**
 * Created only by DAO classes
 */
interface TransactionEntity : Comparable<TransactionData>, TransactionData {
    override val id: Uuid
    override val description: String
    override val timestamp: Instant
    override val transactionType: String
    val clearedById: Uuid?
    val items: List<TransactionItemEntity>
    val budgetId: Uuid
}

interface AllocatedItemEntities {
    val real: List<TransactionItemEntity>
    val charge: List<TransactionItemEntity>
    val draft: List<TransactionItemEntity>
    val category: List<TransactionItemEntity>
    val other: List<TransactionItemEntity>
}

fun TransactionEntity.allocateItemsByAccountType(idToAccount: (Uuid) -> AccountData?): AllocatedItemEntities {
    val real = mutableListOf<TransactionItemEntity>()
    val charge = mutableListOf<TransactionItemEntity>()
    val draft = mutableListOf<TransactionItemEntity>()
    val other = mutableListOf<TransactionItemEntity>()
    val category = mutableListOf<TransactionItemEntity>()
    items.forEach { item: TransactionItemEntity ->
        when (idToAccount(item.accountId)!!.type) {
            AccountType.category.name -> category.add(item)
            AccountType.real.name -> real.add(item)
            AccountType.draft.name -> draft.add(item)
            AccountType.charge.name -> charge.add(item)
            else -> other.add(item)
        }
    }
    return object : AllocatedItemEntities {
        override val real: List<TransactionItemEntity> = real
        override val charge: List<TransactionItemEntity> = charge
        override val draft: List<TransactionItemEntity> = draft
        override val category: List<TransactionItemEntity> = category
        override val other: List<TransactionItemEntity> = other
    }
}

/**
 * Created only by DAO classes
 */
interface TransactionItemEntity {
    val id: Uuid
    val amount: BigDecimal
    val description: String?
    val accountId: Uuid
    val accountType: String
    val draftStatus: String
    val transactionId: Uuid
}

/**
 * Created only by DAO classes
 */
interface AccountTransactionEntity : Comparable<AccountTransactionEntity> {
    val id: Uuid
    val transactionId: Uuid
    val amount: BigDecimal

    // TODO consider getting this out of here since it isn't related to the TransactionDao
    //      Any place it is used, it could be computed by the client.
    val balance: BigDecimal?
    val transactionType: String
    val description: String?
    val transactionDescription: String?
    val accountId: Uuid
    val timestamp: Instant
    val draftStatus: String
    val budgetId: Uuid
}
