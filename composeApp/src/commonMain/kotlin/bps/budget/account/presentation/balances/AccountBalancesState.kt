package bps.budget.account.presentation.balances

import bps.budget.account.domain.Account
import bps.budget.core.presentation.UiText
import bps.budget.model.AccountType

data class AccountBalancesState(
    val accountTypes: List<AccountType> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val selectedTabIndex: Int = 0,
    val selectedAccount: Account? = null,
    val errorMessage: UiText? = null,
)
