package bps.budget.persistence

import bps.budget.model.AccountData
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AccountEntity(
    override val id: Uuid = Uuid.random(),
    override val name: String,
    override val description: String = "",
    override val balance: BigDecimal = BigDecimal.ZERO.setScale(2),
    override val type: String,
    val budgetId: Uuid,
    val companionId: Uuid? = null,
) : AccountData {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccountEntity) return false

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
        return "AccountEntity(id=$id, name='$name', description='$description', balance=$balance, type='$type', budgetId=$budgetId)"
    }

}
