package bps.budget.account.data.network

import bps.budget.core.data.safeCall
import bps.budget.core.domain.DataError
import bps.budget.model.AccountType
import bps.budget.model.AccountsResponse
import bps.kotlin.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class KtorRemoteAccountDataSource(
    private val httpClient: HttpClient,
    // TODO get port and IP from configuration
//    val baseUrl: String = "http://192.168.7.172:8085",
    val baseUrl: String = "http://localhost:8085",
) : RemoteAccountDataSource {

    override suspend fun getAccounts(types: List<AccountType>): Result<AccountsResponse, DataError.Remote> =
        safeCall {
            httpClient.get("$baseUrl/budgets/bffc0cec-a00e-41dc-9325-09e768a08e43/accounts") {
                types.forEach { type ->
                    parameter("type", type.name)
                }
            }
        }

}
