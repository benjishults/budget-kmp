@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.account.presentation.account_detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bps.budget.account.domain.Account
import kotlin.uuid.ExperimentalUuidApi

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
                is AccountDetailAction.OnCompanionSelected -> {
                    TODO()
                }
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
    // TODO I AM HERE.  insert a view of the account details, maybe transactions or a button to see them
    val account: Account = state.account!!
    // TODO show
    //      1. account description and balance
    //      2. link to companion if applicable
    //      3. show transactions button (to new screen?  if so, viewModel could be scoped to the grandparent
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Row(modifier = Modifier) {
            Text(
                text = account.name,
                modifier = Modifier.weight(0.7f),
            )
            Text(
                text = account.balance.toPlainString(),
                modifier = Modifier.weight(0.3f),
                textAlign = TextAlign.Right,
            )
        }
        Text(text = account.description)
        // TODO do I want a companion ID from the API pointing both ways?
        if (account.companionId !== null) {
            Button(
                onClick = { onAction(AccountDetailAction.OnCompanionSelected(account)) },
            ) {
                Text(text = "View Companion")
            }
        }
    }
}
