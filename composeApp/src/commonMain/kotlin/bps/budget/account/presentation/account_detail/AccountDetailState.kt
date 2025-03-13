package bps.budget.account.presentation.account_detail

import bps.budget.account.domain.Account

data class AccountDetailState(
    val isLoading: Boolean = true,
    val account: Account? = null,
) {
}
