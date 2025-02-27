package bps.budget.server.account

import bps.budget.model.AccountType
import bps.budget.model.CategoryAccount
import bps.budget.model.ChargeAccount
import bps.budget.model.DraftAccount
import bps.budget.model.RealAccount
import bps.budget.persistence.jdbc.JdbcAccountDao
import bps.budget.server.model.toResponse
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import java.util.UUID

fun Routing.accountRoutes(accountDao: JdbcAccountDao) {
    get("/budgets/{budgetId}/accounts") {
        call
            .pathParameters
            .getAll("budgetId")
            ?.let { params: List<String> ->
                val budgetId = UUID.fromString(params[0])
                val realAccounts: List<RealAccount> =
                    accountDao.getActiveAccounts(AccountType.real.name, budgetId, RealAccount)
                val categoryAccounts: List<CategoryAccount> =
                    accountDao.getActiveAccounts(AccountType.category.name, budgetId, CategoryAccount)
                val draftAccounts: List<DraftAccount> =
                    accountDao.getActiveAccounts(
                        AccountType.draft.name,
                        budgetId,
                        DraftAccount { companionId ->
                            realAccounts
                                .find { it.id == companionId }!!
                        },
                    )
                val chargeAccounts: List<ChargeAccount> =
                    accountDao.getActiveAccounts(AccountType.charge.name, budgetId, ChargeAccount)
                call.respond(
                    realAccounts.map { it.toResponse() } + categoryAccounts.map { it.toResponse() } + chargeAccounts.map { it.toResponse() } + draftAccounts.map { it.toResponse() },
//                    TypeInfo(AccountResponse::class),
                )
            }
    }
}
