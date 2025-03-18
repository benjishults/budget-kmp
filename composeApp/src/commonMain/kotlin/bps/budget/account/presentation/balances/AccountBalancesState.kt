package bps.budget.account.presentation.balances

import bps.budget.account.domain.Account
import bps.budget.core.presentation.UiText
import bps.budget.model.AccountType

/**
 * State needed for the associated [androidx.lifecycle.ViewModel].
 */
data class AccountBalancesState(
    val accountType: AccountType = AccountType.real,
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTabIndex: Int = 0,
    val selectedAccount: Account? = null,
    val errorMessage: UiText? = null,
)
