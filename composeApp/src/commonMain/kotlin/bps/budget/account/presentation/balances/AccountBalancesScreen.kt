package bps.budget.account.presentation.balances

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bps.budget.account.presentation.balances.components.AccountBalanceList
import bps.budget.model.AccountResponse
//import bps.budget.account.presentation.balances.components.AccountTypeSelector

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
fun AccountBalancesScreen(
    state: AccountBalancesState,
    onAction: (AccountBalancesAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
//        AccountTypeSelector()
        // TODO put type selection here
        // TODO put list of accounts here
        AccountBalanceList(
            accounts = state.accounts,
            onAccountClick = {  },
        )
    }
}
