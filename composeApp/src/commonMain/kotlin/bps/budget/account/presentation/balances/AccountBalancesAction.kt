package bps.budget.account.presentation.balances

import bps.budget.account.domain.Account
import bps.budget.model.AccountType

/**
 * What the user can do on this screen.
 */
sealed interface AccountBalancesAction {
    data class OnAccountTypesChange(
        val type: AccountType,
    ) : AccountBalancesAction {
        val index: Int =
            AccountType
                .entries
                .indexOf(type)
    }

    data class OnAccountSelection(val account: Account) : AccountBalancesAction
}
