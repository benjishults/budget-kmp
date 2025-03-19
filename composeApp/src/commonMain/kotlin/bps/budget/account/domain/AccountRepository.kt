package bps.budget.account.domain

import bps.budget.core.domain.DataError
import bps.budget.model.AccountType
import bps.kotlin.Result

interface AccountRepository {
    suspend fun searchAccounts(types: List<AccountType>): Result<List<Account>, DataError.Remote>
}
