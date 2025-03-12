package bps.budget.account.presentation.balances

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bps.budget.account.domain.Account
import bps.budget.account.domain.AccountRepository
import bps.budget.core.domain.onError
import bps.budget.core.domain.onSuccess
import bps.budget.core.presentation.toUiText
import bps.budget.model.AccountType
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountBalancesViewModel(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountBalancesState())
    val state: StateFlow<AccountBalancesState> = _state
        .onStart {
            if (cachedAccounts.isEmpty()) {
                observeTypeChange()
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            _state.value,
        )
    private var cachedAccounts: List<Account> = emptyList<Account>()
    private var typeSwitchJob: Job? = null

    fun onAction(action: AccountBalancesAction): Unit =
        when (action) {
            is AccountBalancesAction.OnAccountSelection -> {
                _state.update {
                    it.copy(selectedAccount = action.account)
                }
            }
            is AccountBalancesAction.OnAccountTypesChange -> {
                _state.update {
                    it.copy(accountTypes = action.types)
                }
            }
        }

    @OptIn(FlowPreview::class)
    private fun observeTypeChange() {
        state
            .map { accountBalanceState: AccountBalancesState ->
                accountBalanceState.accountTypes
            }
            .distinctUntilChanged()
            .debounce(500L)
            .onEach { query: List<AccountType> ->
                typeSwitchJob?.cancel()
                typeSwitchJob = searchAccounts(query)
//                _state.update {
//                    it.copy(
//                        errorMessage = null,
//                        accountTypes = query,
//                        accounts = cachedAccounts,
//                    )
//                }
            }
            .launchIn(viewModelScope)

    }

    private fun searchAccounts(types: List<AccountType>): Job =
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            accountRepository.searchAccounts(types).onSuccess { results ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        accounts = results,
                        errorMessage = null,
                    )
                }
            }
                .onError { error ->
                    _state.update {
                        it.copy(
                            accounts = emptyList(),
                            isLoading = false,
                            errorMessage = error.toUiText(),
                        )
                    }
                }
        }
}
