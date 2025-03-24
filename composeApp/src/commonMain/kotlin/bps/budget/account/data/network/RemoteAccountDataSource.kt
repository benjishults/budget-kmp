package bps.budget.account.data.network

import bps.budget.core.domain.DataError
import bps.budget.model.AccountType
import bps.budget.model.AccountsResponse
import bps.kotlin.Result

// NOTE this interface isn't doing much for us.  It has to stay within the data layer since it (currently) refers to
//      AccountResponse
interface RemoteAccountDataSource {
    suspend fun getAccounts(
        types: List<AccountType> = emptyList(),
    ): Result<AccountsResponse, DataError.Remote>
}
