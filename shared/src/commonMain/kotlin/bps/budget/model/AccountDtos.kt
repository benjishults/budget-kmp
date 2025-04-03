@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.model

import bps.kotlin.DecimalWithCents
import bps.kotlin.DecimalWithCents_ZERO
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
class AccountResponse(
    val name: String,
    val id: Uuid,
    val type: String,
    val balance: DecimalWithCents,
    val description: String,
    val budgetId: Uuid,
    val companionId: Uuid? = null,
) {
    init {
        require(
            (companionId === null) == (type != AccountType.draft.name),
        )
    }
}

@Serializable
class AccountsResponse(
    val items: List<AccountResponse>,
    val links: Links? = null,
)

/**
 * Used for POST and PUT requests.
 */
@Serializable
class AccountRequest(
    val name: String,
    val type: String,
    val balance: DecimalWithCents = DecimalWithCents_ZERO,
    val description: String = "",
    val budgetId: Uuid,
    val companionId: Uuid? = null,
) {
    init {
        require(
            (companionId === null) == (type !== AccountType.draft.name),
        )
    }
}
