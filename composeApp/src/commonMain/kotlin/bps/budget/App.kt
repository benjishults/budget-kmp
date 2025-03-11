package bps.budget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import bps.budget.account.data.network.KtorRemoteAccountDataSource
import bps.budget.account.data.repository.DefaultAccountRepository
import bps.budget.account.presentation.balances.AccountBalanceScreenRoot
import bps.budget.account.presentation.balances.AccountBalancesViewModel
import bps.budget.core.data.HttpClientFactory
import io.ktor.client.engine.HttpClientEngine

@Composable
//@Preview
fun App(engine: HttpClientEngine) {
    AccountBalanceScreenRoot(
        viewModel = remember {
            AccountBalancesViewModel(
                DefaultAccountRepository(
                    KtorRemoteAccountDataSource(
                        httpClient = HttpClientFactory.create(engine),
                    ),
                ),
            )
        },
        onAccountClick = {},
    )
}
