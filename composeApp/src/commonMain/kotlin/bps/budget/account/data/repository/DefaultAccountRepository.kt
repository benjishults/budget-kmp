package bps.budget.account.data.repository

import bps.budget.account.data.mapper.toDomainAccount
import bps.budget.account.data.network.RemoteAccountDataSource
import bps.budget.account.domain.Account
import bps.budget.account.domain.AccountRepository
import bps.budget.core.domain.DataError
import bps.budget.core.domain.Result
import bps.budget.core.domain.map
import bps.budget.model.AccountType
import bps.budget.model.AccountsResponse

class DefaultAccountRepository(
    private val dataSource: RemoteAccountDataSource,
) : AccountRepository {
    override suspend fun searchAccounts(types: List<AccountType>): Result<List<Account>, DataError.Remote> {
        return dataSource
            .getAccounts(types)
            .map { successResponse: AccountsResponse ->
                successResponse
                    .items
                    .map { it.toDomainAccount() }
            }
    }
}

