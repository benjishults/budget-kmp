package bps.budget.account.presentation

import androidx.lifecycle.ViewModel
import bps.budget.account.domain.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Intended to share data between children of [bps.budget.app.Route.AccountGraph] in the [androidx.navigation.NavGraph].
 */
class SelectedAccountViewModel : ViewModel() {

    private val _selectedAccount: MutableStateFlow<Account?> = MutableStateFlow<Account?>(null)
    val selectedAccount: StateFlow<Account?> = _selectedAccount.asStateFlow()

    fun onSelectAccount(account: Account?) {
        _selectedAccount.value = account
    }

}
