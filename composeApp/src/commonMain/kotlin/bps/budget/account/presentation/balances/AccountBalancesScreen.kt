package bps.budget.account.presentation.balances

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bps.budget.model.AccountResponse
import androidx.compose.runtime.getValue

@Composable
fun AccountBalanceScreenRoot(
    viewModel: AccountBalancesViewModel,
    onAccountClick: (AccountResponse) -> Unit,
) {
    val state: AccountBalancesState by viewModel.state.collectAsStateWithLifecycle()

    AccountBalancesScreen(
        state = state,
        onAction = { action: AccountBalancesAction ->
            when (action) {
                is AccountBalancesAction.OnAccountSelection -> {
                    onAccountClick(action.account)
                }
                is AccountBalancesAction.OnAccountTypesChange -> TODO()
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun AccountBalancesScreen(
    state: AccountBalancesState,
    onAction: (AccountBalancesAction) -> Unit,
) {
}
