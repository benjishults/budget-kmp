package bps.budget.account.presentation.account_detail

import bps.budget.account.domain.Account

/**
 * State needed for the associated [androidx.lifecycle.ViewModel].
 */
data class AccountDetailState(
    val isLoading: Boolean = true,
    val account: Account? = null,
)
