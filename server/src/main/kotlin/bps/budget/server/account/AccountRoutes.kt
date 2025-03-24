@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.server.account

import bps.budget.model.AccountType
import bps.budget.model.AccountsResponse
import bps.budget.persistence.AccountDao
import bps.budget.persistence.AccountEntity
import bps.budget.server.core.ifTypeWanted
import bps.budget.server.model.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun Routing.accountRoutes(accountDao: AccountDao) {
    get("/budgets/{budgetId}/accounts") {
        val types: List<AccountType> =
            call.queryParameters.getAll("type")
                ?.map {
                    try {
                        AccountType.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "query parameter 'type' must be one of 'real', 'category', 'charge', or 'draft': ${e.message}",
                        )
                        // NOTE non-local exit!
                        return@get
                    }
                }
                ?: emptyList()
        val budgetId = call.pathParameters["budgetId"]
        if (budgetId === null)
            call.respond(HttpStatusCode.BadRequest)
        else {
            returnAccounts(
                budgetId = Uuid.parse(budgetId),
                accountDao = accountDao,
                types = types,
            )
        }
    }
    get("/budgets/{budgetId}/accounts/{accountId}") {
        val accountId = call.pathParameters["accountId"]
        val budgetId = call.pathParameters["budgetId"]
        if (accountId === null || budgetId === null) {
            call.respond(HttpStatusCode.BadRequest)
        } else {
            getAccount(
                accountId = Uuid.parse(accountId),
                budgetId = Uuid.parse(budgetId),
                accountDao = accountDao,
            )
        }
    }
}

private suspend fun RoutingContext.getAccount(
    accountId: Uuid,
    budgetId: Uuid,
    accountDao: AccountDao,
) {
    val account: AccountEntity? =
        accountDao.getAccountOrNull(accountId, budgetId)
    if (account == null) {
        call.respond(HttpStatusCode.NotFound)
    } else {
        call.respond(account.toResponse())
    }
}

private suspend fun RoutingContext.returnAccounts(
    budgetId: Uuid,
    accountDao: AccountDao,
    types: List<AccountType> = emptyList(),
) {
    val realAccounts: List<AccountEntity> =
        ifTypeWanted(AccountType.real, types) {
            accountDao.getActiveAccounts(AccountType.real.name, budgetId)
        }
    val categoryAccounts: List<AccountEntity> =
        ifTypeWanted(AccountType.category, types) {
            accountDao.getActiveAccounts(AccountType.category.name, budgetId)
        }
    val draftAccounts: List<AccountEntity> =
        ifTypeWanted(AccountType.draft, types) {
            accountDao.getActiveAccounts(
                AccountType.draft.name,
                budgetId,
                /*                DraftAccount { companionId ->
                                    (realAccounts
                                        .takeIf { it.isNotEmpty() }
                                        ?: accountDao.getActiveAccounts(AccountType.real.name, budgetId, RealAccount))
                                        .find { it.id == companionId }!!
                                }*/
            )
        }
    val chargeAccounts: List<AccountEntity> =
        ifTypeWanted(AccountType.charge, types) {
            accountDao.getActiveAccounts(AccountType.charge.name, budgetId)
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
