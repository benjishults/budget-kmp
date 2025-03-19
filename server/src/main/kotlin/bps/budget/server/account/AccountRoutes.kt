@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.server.account

import bps.budget.model.Account
import bps.budget.model.AccountType
import bps.budget.model.AccountsResponse
import bps.budget.model.CategoryAccount
import bps.budget.model.ChargeAccount
import bps.budget.model.DraftAccount
import bps.budget.model.RealAccount
import bps.budget.persistence.AccountDao
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
        call.queryParameters["type"]
            ?.let {
                val type: AccountType = try {
                    AccountType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "query parameter 'type' must be one of 'real', 'category', 'charge', or 'draft': ${e.message}",
                    )
                    // NOTE non-local exit!
                    return@get
                }
                val accountId = call.pathParameters["accountId"]
                val budgetId = call.pathParameters["budgetId"]
                if (accountId === null || budgetId === null) {
                    call.respond(HttpStatusCode.BadRequest)
                } else {
                    getAccount(
                        accountId = Uuid.parse(accountId),
                        type = type,
                        budgetId = Uuid.parse(budgetId),
                        accountDao = accountDao,
                    )
                }
            }
            ?: call.respond(HttpStatusCode.BadRequest, "query parameter 'type' is required")
    }
}

private suspend fun RoutingContext.getAccount(
    accountId: Uuid,
    type: AccountType,
    budgetId: Uuid,
    accountDao: AccountDao,
) {
    val account: Account? =
        when (type) {
            AccountType.category ->
                accountDao.getCategoryAccountOrNull(
                    accountId,
                    budgetId,
                )
            AccountType.real ->
                accountDao.getRealAccountOrNull(accountId, budgetId)
            AccountType.draft ->
                accountDao.getDraftAccountOrNull(
                    accountId,
                    budgetId,
                )
            AccountType.charge ->
                accountDao.getChargeAccountOrNull(
                    accountId,
                    budgetId,
                )
        }
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
    val realAccounts: List<RealAccount> =
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
