package bps.budget.account.presentation.balances

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bps.budget.account.domain.Account
import bps.budget.account.presentation.balances.components.AccountBalanceList
import bps.budget.model.AccountType

@Composable
fun AccountBalanceScreenRoot(
    viewModel: AccountBalancesViewModel,
    onAccountClick: (Account) -> Unit,
) {
    val state: AccountBalancesState by viewModel.state.collectAsStateWithLifecycle()

    AccountBalancesScreen(
        state = state,
        onAction = { action: AccountBalancesAction ->
            when (action) {
                is AccountBalancesAction.OnAccountSelection -> {
                    onAccountClick(action.account)
                }
                is AccountBalancesAction.OnAccountTypesChange -> {
                    // NOTE viewModel seems to take care of this
                }
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
    val pagerState: PagerState = rememberPagerState { AccountType.entries.size }
    val selectedAccountsLazyListState: LazyListState = rememberLazyListState()

    LaunchedEffect(state.accounts) {
        selectedAccountsLazyListState.animateScrollToItem(0)
    }
    LaunchedEffect(state.selectedTabIndex) {
        pagerState.animateScrollToPage(state.selectedTabIndex)
    }
    LaunchedEffect(pagerState.currentPage) {
        onAction(AccountBalancesAction.OnAccountTypesChange(AccountType.entries[pagerState.currentPage]))
    }
//        Surface(
//            modifier = Modifier
//                .fillMaxWidth()
//                .fillMaxHeight(),
//            color = MaterialTheme.colorScheme.surface,
//            shape = RoundedCornerShape(topStart = 32.dp,
//                topEnd = 32.dp)
//        ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
//        AccountTypeSelector()
        // TODO: longer term I might want a multi-line row of selectable types so that we can see more than one type at a time.
        TabRow(
            selectedTabIndex = state.selectedTabIndex,
            modifier = Modifier
                .widthIn(max = 700.dp)
                .padding(horizontal = 12.dp)
                .fillMaxWidth(),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[state.selectedTabIndex]),
                )
            },
        ) {
            AccountType
                .entries
                .forEachIndexed { index, type: AccountType ->
                    Tab(
                        selected = index == state.selectedTabIndex,
                        onClick = { onAction(AccountBalancesAction.OnAccountTypesChange(type)) },
                        modifier = Modifier.weight(1f),
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.secondary,
                    ) {
                        Text(
                            text = type.name,
                            modifier = Modifier
                                .padding(vertical = 12.dp),
                        )
                    }
                }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { pageIndex ->
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            )
            {            // TODO use pageIndex as index in AccountType enum to select which accounts to show?
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    when {
                        state.errorMessage !== null -> {
                            Text(
                                text = state.errorMessage.asString(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        state.accounts.isEmpty() -> {
                            Text(
                                text = "No accounts of that type available.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        else -> {
                            AccountBalanceList(
                                accounts = state.accounts,
                                onAccountClick = { account: Account ->
                                    onAction(AccountBalancesAction.OnAccountSelection(account))
                                },
                                modifier = Modifier.fillMaxSize(),
                                scrollState = selectedAccountsLazyListState,
                            )
                        }
                        //
                    }
                }
            }
        }
    }

}
