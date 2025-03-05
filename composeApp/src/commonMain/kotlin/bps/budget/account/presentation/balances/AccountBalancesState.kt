package bps.budget.account.presentation.balances

import bps.budget.core.presentation.UiText
import bps.budget.model.AccountResponse
import bps.budget.model.AccountType

data class AccountBalancesState(
    val accountTypes: List<AccountType> = emptyList(),
    val accounts: List<AccountResponse> = emptyList(),
    val isLoading: Boolean = false,
    val selectedAccount: AccountResponse? = null,
    val errorMessage: UiText? = null,
)
