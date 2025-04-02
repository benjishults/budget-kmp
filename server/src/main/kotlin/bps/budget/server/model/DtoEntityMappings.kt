@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.server.model

import bps.budget.model.AccountResponse
import bps.budget.model.AccountTransactionResponse
import bps.budget.model.AccountType
import bps.budget.model.TransactionItemResponse
import bps.budget.model.TransactionResponse
import bps.budget.persistence.AccountEntity
import bps.budget.persistence.AccountTransactionEntity
import bps.budget.persistence.TransactionEntity
import bps.budget.persistence.TransactionItemEntity
import bps.kotlin.DecimalWithCents
import bps.kotlin.buildDecimalWithCents
import kotlin.uuid.ExperimentalUuidApi

fun AccountEntity.toResponse(): AccountResponse =
    when (this.type) {
        AccountType.draft.name ->
            AccountResponse(
                name = name,
                id = id,
                type = type,
                balance = DecimalWithCents(balance.toPlainString()),
                description = description,
                budgetId = budgetId,
                companionId = companionId,
            )
        else ->
            AccountResponse(
                name,
                id,
                type,
                DecimalWithCents(balance.toPlainString()),
                description,
                budgetId,
            )
    }

fun AccountTransactionEntity.toResponse(): AccountTransactionResponse =
    AccountTransactionResponse(
        transactionItemId = id,
        transactionId = transactionId,
        timestamp = timestamp,
        description = description ?: transactionDescription,
        amount = buildDecimalWithCents(amount.toPlainString()),
        balance = balance?.let { buildDecimalWithCents(it.toPlainString()) },
        type = transactionType,
        accountId = accountId,
        budgetId = budgetId,
    )

fun TransactionEntity.toResponse(): TransactionResponse =
    TransactionResponse(
        id = id,
        timestamp = timestamp,
        description = description,
        type = transactionType,
        budgetId = budgetId,
        items = items.map { it.toResponse() },
        clearedById = clearedById,
    )

fun TransactionItemEntity.toResponse(): TransactionItemResponse =
    TransactionItemResponse(
        id = id,
        description = description,
        amount = buildDecimalWithCents(amount.toPlainString()),
        draftStatus = draftStatus,
        accountId = accountId,
    )
