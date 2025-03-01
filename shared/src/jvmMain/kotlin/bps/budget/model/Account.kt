@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.model

import bps.serialization.BigDecimalNumericSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
class AccountResponse(
    val name: String,
    val id: Uuid,
    val type: AccountType,
    @Serializable(with = BigDecimalNumericSerializer::class)
    val balance: BigDecimal,
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

/**
 * Used for POST and PUT requests.
 */
@Serializable
class AccountRequest(
    val name: String,
    val type: AccountType,
    @Serializable(with = BigDecimalNumericSerializer::class)
    val balance: BigDecimal = BigDecimal.ZERO.setScale(2),
    val description: String = "",
    val budgetId: Uuid,
    val companionId: Uuid? = null,
) {
    init {
        require(
            (companionId === null) == (type !== AccountType.draft),
        )
    }
}
