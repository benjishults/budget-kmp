@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.server.model

import bps.budget.model.Account
import bps.budget.model.AccountResponse
import bps.budget.model.AccountType
import bps.budget.model.DraftAccount
import kotlin.uuid.ExperimentalUuidApi

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
