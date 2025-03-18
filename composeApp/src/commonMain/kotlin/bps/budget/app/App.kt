package bps.budget.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import bps.budget.account.domain.Account
import bps.budget.account.presentation.SelectedAccountViewModel
import bps.budget.account.presentation.account_detail.AccountDetailAction
import bps.budget.account.presentation.account_detail.AccountDetailScreenRoot
import bps.budget.account.presentation.account_detail.AccountDetailViewModel
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
                composable<Route.AccountBalanceList>(
                    typeMap = mapOf(typeOf<Uuid>() to UuidNavType),
                ) { backStackEntry: NavBackStackEntry ->
                    // NOTE a NavBackStackEntry has a Bundle which contains the Route.AccountBalanceList
                    val viewModel: AccountBalancesViewModel = koinViewModel<AccountBalancesViewModel>()
                    // NOTE initialize the selectedAccountViewModel to be shared with the Route.AccountDetail
                    val selectedAccountViewModel: SelectedAccountViewModel =
                        backStackEntry.ancestorSharedKoinViewModel<SelectedAccountViewModel>(navController)

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
                composable<Route.AccountDetail>(
                    typeMap = mapOf(typeOf<Uuid>() to UuidNavType),
                ) { backStackEntry: NavBackStackEntry ->
                    // NOTE a NavBackStackEntry has a Bundle which contains the Route.AccountDetail
                    // NOTE the selectedAccountViewModel will be remembered from the AccountBalanceList initialization
                    val selectedAccountViewModel: SelectedAccountViewModel =
                        backStackEntry.ancestorSharedKoinViewModel<SelectedAccountViewModel>(navController)
                    // NOTE options:
                    //      1. use a special viewModel with the account info (as done with SelectedAccountViewModel)
                    //      2. pass a lot of data through the route (limited size of bundle is a downside)
                    //      3. use a local cache
                    // NOTE here just to remind me of what's possible
//                    val accountDetailRoute: Route.AccountDetail = backStackEntry.toRoute<Route.AccountDetail>()
//                    val accountId: Uuid = accountDetailRoute.id
                    val selectedAccount: Account? by selectedAccountViewModel.selectedAccount.collectAsStateWithLifecycle()

                    val accountDetailViewModel = koinViewModel<AccountDetailViewModel>()
                    LaunchedEffect(selectedAccount) {
                        selectedAccount
                            ?.let {
                                accountDetailViewModel.onAction(AccountDetailAction.OnSelectedAccountChanged(it))
                                // TODO needed?
//                                selectedAccountViewModel.onSelectAccount(selectedAccount)
                            }
                    }

                    AccountDetailScreenRoot(
                        viewModel = accountDetailViewModel,
                        onBackClick = {
                            navController.navigateUp()
                            selectedAccountViewModel.onSelectAccount(null)
                        },
                    )
//                    Box(
//                        modifier = Modifier.fillMaxSize(),
//                        contentAlignment = Alignment.Center,
//                    ) {
//                        Text(text = "Account Detail: ID is $selectedAccount")
//                    }
                }
            }
        }
    }
}

/**
 * @return the [ViewModel] of the parent [NavBackStackEntry] if there is a parent, otherwise the [ViewModel] of the
 * desired type from the [org.koin.core.Koin] registry.
 * @param ancestorIndex positive number of parent references to follow.  Defaults to `1`.
 */
@Composable
private inline fun <reified T : ViewModel> NavBackStackEntry.ancestorSharedKoinViewModel(
    navController: NavController,
    ancestorIndex: Int = 1,
): T {
    require(ancestorIndex > 0) { "ancestorIndex must be positive" }
    val (navDestination: NavDestination, index: Int) =
        generateSequence(this.destination to ancestorIndex) { (navDestination: NavDestination?, index: Int) ->
            // NOTE if there is a parent and index is not zero, then continue.
            //      If index is zero on the last value then the navDestination is the one we are after.
            //      If index is not zero on the last value, then there weren't enough ancestors.
            if (index == 0)
                null
            else
                navDestination
                    ?.parent
                    ?.let { parent: NavDestination ->
                        parent to index - 1
                    }
        }
            .last()
    return if (index == 0) {
        val targetAncestorRoute: String? =
            navDestination.route
        if (targetAncestorRoute !== null) {
            val parentBackStackEntry: NavBackStackEntry = navController.getBackStackEntry(targetAncestorRoute)
            koinViewModel(
                viewModelStoreOwner =
                    // NOTE make a new ViewModel only when this is called on a different receiver (child) NavBackStackEntry.
                    //      No need to recreate it if we visit the same Account immediately.
                    remember(this) {
                        // NOTE scoping the ViewModel to the parent NavBackStackEntry
                        parentBackStackEntry
                    },
            )
        } else {
            koinViewModel<T>()
        }
    } else {
        koinViewModel<T>()
    }

}
