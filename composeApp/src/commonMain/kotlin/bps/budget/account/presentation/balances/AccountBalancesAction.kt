package bps.budget.account.presentation.balances

import bps.budget.account.domain.Account
import bps.budget.model.AccountType

sealed interface AccountBalancesAction {
    data class OnAccountTypesChange(val types: List<AccountType>) : AccountBalancesAction
    data class OnAccountSelection(val account: Account) : AccountBalancesAction
}
