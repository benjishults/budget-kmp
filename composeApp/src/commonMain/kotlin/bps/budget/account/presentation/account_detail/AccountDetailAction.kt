package bps.budget.account.presentation.account_detail

import bps.budget.account.domain.Account

/**
 * What the user can do on this screen.
 */
sealed interface AccountDetailAction {

    data object OnBackClick : AccountDetailAction

    // NOTE may want to show recent transactions always so this won't be an action.
    data object OnViewTransactionsClick : AccountDetailAction

    data class OnSelectedAccountChanged(val account: Account) : AccountDetailAction

    data class OnCompanionSelected(val companionAccount: Account) : AccountDetailAction

}
