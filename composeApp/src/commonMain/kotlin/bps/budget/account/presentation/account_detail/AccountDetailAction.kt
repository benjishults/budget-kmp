package bps.budget.account.presentation.account_detail

import bps.budget.account.domain.Account

sealed interface AccountDetailAction {
    data object OnBackClick : AccountDetailAction
    data class OnSelectedAccountChanged(val account: Account) : AccountDetailAction
}
