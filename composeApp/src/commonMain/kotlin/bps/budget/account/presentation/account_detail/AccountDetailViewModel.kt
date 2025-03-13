package bps.budget.account.presentation.account_detail

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountDetailViewModel : ViewModel() {

    private val _state: MutableStateFlow<AccountDetailState> = MutableStateFlow(AccountDetailState())
    val state: StateFlow<AccountDetailState> = _state.asStateFlow()

    fun onAction(action: AccountDetailAction) {

    }
}
