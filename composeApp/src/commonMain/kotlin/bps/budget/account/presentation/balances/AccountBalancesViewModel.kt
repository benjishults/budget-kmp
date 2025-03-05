package bps.budget.account.presentation.balances

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AccountBalancesViewModel : ViewModel() {

    private val _state = MutableStateFlow(AccountBalancesState())
    val state = _state.asStateFlow()

    fun onAction(action: AccountBalancesAction): Unit =
        when (action) {
            is AccountBalancesAction.OnAccountSelection -> {
                _state.update { it.copy(selectedAccount = action.account) }
            }
            is AccountBalancesAction.OnAccountTypesChange -> {
                _state.update { it.copy(accountTypes = action.types) }
            }
        }

}
