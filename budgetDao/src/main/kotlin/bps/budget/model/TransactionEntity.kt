package bps.budget.model

import kotlinx.datetime.Instant
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class TransactionEntity(
    val id: Uuid,
    val description: String,
    val timestamp: Instant,
    val transactionType: TransactionType,
    val clearedBy: Transaction? = null,
    val items: List<TransactionItemEntity>?,
    val budgetId: Uuid,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransactionEntity) return false

        if (id != other.id) return false
        if (budgetId != other.budgetId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + budgetId.hashCode()
        return result
    }

    override fun toString(): String {
        return "TransactionEntity(id=$id, description='$description', timestamp=$timestamp, transactionType=$transactionType, clearedBy=$clearedBy, items=$items, budgetId=$budgetId)"
    }

}

@OptIn(ExperimentalUuidApi::class)
class TransactionItemEntity(
    val id: Uuid,
    val amount: BigDecimal,
    val description: String?,
    val account: AccountEntity,
    val timestamp: Instant,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransactionItemEntity) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "TransactionItemEntity(id=$id, amount=$amount, description=$description, account=$account, timestamp=$timestamp)"
    }

}
