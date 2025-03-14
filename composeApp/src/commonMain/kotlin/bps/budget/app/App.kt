package bps.budget.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import bps.budget.account.domain.Account
import bps.budget.account.presentation.SelectedAccountViewModel
import bps.budget.account.presentation.balances.AccountBalanceScreenRoot
import bps.budget.account.presentation.balances.AccountBalancesViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.reflect.typeOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun App() {
    MaterialTheme {
        val navController: NavHostController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = Route.AccountGraph,
        ) {
            navigation<Route.AccountGraph>(
                startDestination = Route.AccountBalanceList,
            ) {
                composable<Route.AccountBalanceList>(typeMap = mapOf(typeOf<Uuid>() to UuidNavType)) { backStackEntry ->
                    val viewModel: AccountBalancesViewModel = koinViewModel<AccountBalancesViewModel>()
                    val selectedAccountViewModel: SelectedAccountViewModel =
                        backStackEntry.sharedKoinViewModel<SelectedAccountViewModel>(navController)

                    LaunchedEffect(true) {
                        selectedAccountViewModel.onSelectAccount(null)
                    }

                    AccountBalanceScreenRoot(
                        viewModel = viewModel,
                        onAccountClick = { account: Account ->
                            selectedAccountViewModel.onSelectAccount(account)
                            navController.navigate(Route.AccountDetail(account.id))
                        },
                    )
                }
                composable<Route.AccountDetail>(typeMap = mapOf(typeOf<Uuid>() to UuidNavType)) { backStackEntry: NavBackStackEntry ->
                    val selectedAccountViewModel: SelectedAccountViewModel =
                        backStackEntry.sharedKoinViewModel<SelectedAccountViewModel>(navController)
                    val selectedAccount: Account? by selectedAccountViewModel.selectedAccount.collectAsStateWithLifecycle()
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "Account Detail: ID is $selectedAccount")
                    }

                    //
                }
            }
        }
    }
}

/**
 * @return the [ViewModel] of the parent [NavBackStackEntry] if there is a parent, otherwise the [ViewModel] of the
 * desired type from the [org.koin.core.Koin] registry.
 */
@Composable
private inline fun <reified T : ViewModel> NavBackStackEntry.sharedKoinViewModel(
    navController: NavController,
): T {
    val navGraphRoute: String? =
        this.destination.parent?.route
    return if (navGraphRoute !== null) {
        koinViewModel(
            viewModelStoreOwner =
                remember(this) {
                    navController.getBackStackEntry(navGraphRoute)
                },
        )
    } else {
        koinViewModel<T>()
    }
}
