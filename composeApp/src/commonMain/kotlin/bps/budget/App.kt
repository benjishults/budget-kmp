package bps.budget

import androidx.compose.runtime.Composable
import bps.budget.account.presentation.balances.AccountBalanceScreenRoot
import bps.budget.account.presentation.balances.AccountBalancesViewModel

@Composable
fun App(viewModel: AccountBalancesViewModel) {
    AccountBalanceScreenRoot(
        viewModel = viewModel,
        onAccountClick = {},
    )
}
