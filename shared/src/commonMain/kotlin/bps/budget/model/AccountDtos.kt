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

@Serializable
class AccountsResponse(
    val items: List<AccountResponse>,
    val links: Links? = null,
)

@Serializable
class Links(
    val next: String? = null,
    val previous: String? = null,
)

/**
 * Used for POST and PUT requests.
 */
@Serializable
class AccountRequest(
    val name: String,
    val type: AccountType,
    val balance: DecimalWithCents = DecimalWithCents_ZERO,
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
