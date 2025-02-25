package bps.budget.server.account

import bps.budget.model.Account
import bps.budget.model.AccountType
import bps.budget.model.CategoryAccount
import bps.budget.model.ChargeAccount
import bps.budget.model.RealAccount
import bps.budget.persistence.jdbc.JdbcAccountDao
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.util.reflect.TypeInfo
import kotlin.uuid.Uuid

fun Routing.accountRoutes(accountDao: JdbcAccountDao) {
    get("/budgets/{budgetId}/accounts") {
        call
            .pathParameters
            .getAll("budgetId")
            ?.let { params: List<String> ->
                val budgetId = Uuid.parse(params[0])
                val realAccounts: List<RealAccount> =
                    accountDao.getActiveAccounts(AccountType.real.name, budgetId, ::RealAccount)
                val categoryAccounts: List<CategoryAccount> =
                    accountDao.getActiveAccounts(AccountType.category.name, budgetId, ::CategoryAccount)
                val chargeAccounts: List<ChargeAccount> =
                    accountDao.getActiveAccounts(AccountType.charge.name, budgetId, ::ChargeAccount)
                call.respond(realAccounts + categoryAccounts + chargeAccounts, TypeInfo(Account::class))
            }
            ?: call.respond(HttpStatusCode.BadRequest)
    }
}
