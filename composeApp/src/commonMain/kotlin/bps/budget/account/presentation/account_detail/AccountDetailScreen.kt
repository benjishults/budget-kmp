package bps.budget.account.presentation.account_detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AccountDetailScreenRoot(
    viewModel: AccountDetailViewModel,
    onBackClick: () -> Unit,
) {
    val state: AccountDetailState by viewModel.state.collectAsStateWithLifecycle()
    AccountDetailScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is AccountDetailAction.OnBackClick -> onBackClick()
                is AccountDetailAction.OnSelectedAccountChanged -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun AccountDetailScreen(
    state: AccountDetailState,
    onAction: (AccountDetailAction) -> Unit,
) {
//      TODO I AM HERE.  insert a view of the account details, maybe transactions or a button to see them
}
