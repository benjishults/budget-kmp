@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.server.transaction

import bps.budget.model.AccountType
import bps.budget.model.AccountsResponse
import bps.budget.model.CategoryAccount
import bps.budget.model.ChargeAccount
import bps.budget.model.DraftAccount
import bps.budget.model.RealAccount
import bps.budget.model.TransactionType
import bps.budget.persistence.TransactionDao
import bps.budget.server.core.RequestError
import bps.budget.server.core.extractQueryParamEnumValuesOrNull
import bps.budget.server.core.ifTypeWanted
import bps.budget.server.model.toResponse
import bps.kotlin.onError
import bps.kotlin.onSuccess
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val defaultPageSize = 100

private const val defaultOffset = 0

fun Routing.transactionRoutes(transactionDao: TransactionDao) {
    get("/budgets/{budgetId}/accounts/{accountId}/transactions") {
        call.extractQueryParamEnumValuesOrNull<TransactionType>("type") {
            "all values of query parameter 'type' must be one of 'real', 'category', 'charge', or 'draft'"
        }
            .onSuccess { types: List<TransactionType> ->
                val budgetId = call.pathParameters["budgetId"]
                val accountId = call.pathParameters["accountId"]
                if (budgetId === null || accountId == null) {
                    call.respond(HttpStatusCode.BadRequest)
                } else {
                    try {
                        val limit: Int = call.request.queryParameters["limit"]?.toInt() ?: defaultPageSize
                        val offset: Int = call.request.queryParameters["offset"]?.toInt() ?: defaultOffset
                        if (limit < 1) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                "limit must be greater than or equal to 1, if provided",
                            )
                        } else if (offset < 0) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                "offset must be greater than or equal to 0, if provided",
                            )
                        } else
                            returnAccountTransactions(
                                accountId = Uuid.parse(accountId),
                                budgetId = Uuid.parse(budgetId),
                                transactionDao = transactionDao,
                                types = types,
                            )
                    } catch (e: NumberFormatException) {
                        if (coroutineContext.isActive) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                "limit and offsets must be integers if provided",
                            )
                        } else
                            throw e
                    }
                }
            }
            .onError { requestError: RequestError ->
                call.respond(
                    HttpStatusCode.BadRequest,
                    requestError.message,
                )
            }
    }

    get("/budgets/{budgetId}/transactions/{transactionId}") {
        val budgetId = call.pathParameters["budgetId"]
        val transactionId = call.parameters["transactionId"]
        if (budgetId === null || transactionId === null) {
            call.respond(HttpStatusCode.BadRequest)
        } else {
            TODO()
//            getTransactions(
//                budgetId = Uuid.parse(budgetId),
//                accountDao = accountDao,
//            )
        }
    }
}

private fun RoutingContext.returnAccountTransactions(
    accountId: Uuid,
    budgetId: Uuid,
    transactionDao: TransactionDao,
    types: List<TransactionType> = emptyList(),
) {
    val realAccounts: List<RealAccount> =
        transactionDao.fetchTransactionItemsInvolvingAccount(
            account = TODO(),
            limit = TODO(),
            offset = TODO(),
            types = TODO(),
            balanceAtEndOfPage = TODO()
        )
        ifTypeWanted(AccountType.real, types) {
            accountDao.getActiveAccounts(AccountType.real.name, budgetId, RealAccount)
        }
    val categoryAccounts: List<CategoryAccount> =
        ifTypeWanted(AccountType.category, types) {
            accountDao.getActiveAccounts(AccountType.category.name, budgetId, CategoryAccount)
        }
    val draftAccounts: List<DraftAccount> =
        ifTypeWanted(AccountType.draft, types) {
            accountDao.getActiveAccounts(
                AccountType.draft.name,
                budgetId,
                DraftAccount { companionId ->
                    (realAccounts
                        .takeIf { it.isNotEmpty() }
                        ?: accountDao.getActiveAccounts(AccountType.real.name, budgetId, RealAccount))
                        .find { it.id == companionId }!!
                },
            )
        }
    val chargeAccounts: List<ChargeAccount> =
        ifTypeWanted(AccountType.charge, types) {
            accountDao.getActiveAccounts(AccountType.charge.name, budgetId, ChargeAccount)
        }
    call.respond(
        AccountsResponse(
            items =
                realAccounts.map { it.toResponse() } +
                        categoryAccounts.map { it.toResponse() } +
                        chargeAccounts.map { it.toResponse() } +
                        draftAccounts.map { it.toResponse() },
        ),
    )
}
