@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.server.model

import bps.budget.model.Account
import bps.budget.model.AccountType
import bps.budget.model.DraftAccount
import bps.serialization.BigDecimalNumericSerializer
import bps.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
class AccountResponse(
    val name: String,
//    @Serializable(with = UUIDSerializer::class)
    val id: Uuid,
    val type: AccountType,
    @Serializable(with = BigDecimalNumericSerializer::class)
    val balance: BigDecimal,
    val description: String,
//    @Serializable(with = UUIDSerializer::class)
    val budgetId: Uuid,
//    @Serializable(with = UUIDSerializer::class)
    val companionId: Uuid? = null,
) {
    init {
        require(
            (companionId === null) == (type !== AccountType.draft),
        )
    }
}

fun Account.toResponse(): AccountResponse =
    when (this) {
        is DraftAccount ->
            AccountResponse(
                name,
                id,
                AccountType.valueOf(type),
                balance,
                description,
                budgetId,
                realCompanion.id,
            )
        else ->
            AccountResponse(name, id, AccountType.valueOf(type), balance, description, budgetId)
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
//    @Serializable(with = UUIDSerializer::class)
    val budgetId: Uuid,
//    @Serializable(with = UUIDSerializer::class)
    val companionId: Uuid? = null,
) {
    init {
        require(
            (companionId === null) == (type !== AccountType.draft),
        )
    }
}
