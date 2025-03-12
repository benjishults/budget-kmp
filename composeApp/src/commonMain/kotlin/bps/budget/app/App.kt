package bps.budget.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import bps.budget.account.domain.Account
import bps.budget.account.presentation.balances.AccountBalanceScreenRoot
import bps.budget.account.presentation.balances.AccountBalancesViewModel
import kotlin.reflect.typeOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun App(viewModel: AccountBalancesViewModel) {
    MaterialTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = Route.AccountGraph,
        ) {
            navigation<Route.AccountGraph>(
                startDestination = Route.AccountBalanceList,
            ) {
                composable<Route.AccountBalanceList>(typeMap = mapOf(typeOf<Uuid>() to UuidNavType)) {
                    AccountBalanceScreenRoot(
                        viewModel = viewModel,
                        onAccountClick = { account: Account ->
                            navController.navigate(Route.AccountDetail(account.id))
                        },
                    )
                }
                composable<Route.AccountDetail>(typeMap = mapOf(typeOf<Uuid>() to UuidNavType)) { entry: NavBackStackEntry ->
                    val args: Route.AccountDetail = entry.toRoute<Route.AccountDetail>()
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "Account Detail: ID is ${args.id}")
                    }

                    //
                }
            }
        }
    }
}
