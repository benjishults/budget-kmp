package bps.budget.account.data.mapper

import bps.budget.account.domain.Account
import bps.budget.model.AccountResponse
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun AccountResponse.toDomainAccount(): Account =
    Account(
        id = id,
        name = name,
        description = description,
        type = type,
        budgetId = budgetId,
        balance = balance,
        companionId = companionId,
    )

