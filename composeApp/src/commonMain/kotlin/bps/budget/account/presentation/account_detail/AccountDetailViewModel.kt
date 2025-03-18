package bps.budget.account.presentation.account_detail

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AccountDetailViewModel : ViewModel() {

    private val _state: MutableStateFlow<AccountDetailState> = MutableStateFlow(AccountDetailState())
    val state: StateFlow<AccountDetailState> = _state.asStateFlow()

    fun onAction(action: AccountDetailAction) {
        when (action) {
            // FIXME
            AccountDetailAction.OnBackClick -> Unit
            // FIXME
            is AccountDetailAction.OnCompanionSelected -> Unit
            is AccountDetailAction.OnSelectedAccountChanged -> {
                _state.update {
                    it.copy(account = action.account)
                }
            }
            // FIXME
            AccountDetailAction.OnViewTransactionsClick -> Unit
        }

    }
}
