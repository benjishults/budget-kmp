@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.account.presentation.account_detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
                is AccountDetailAction.OnBackClick ->
                    onBackClick()
                // NOTE everything else is handled by the ViewModel
                else -> Unit
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
    Column(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = { onAction(AccountDetailAction.OnBackClick) },
            modifier = Modifier
                .align(Alignment.Start)
                .padding(
                    top = 16.dp,
                    start = 16.dp,
                ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
            )
        }
        val account: Account? = state.account
        if (account === null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            // TODO show
            //      1. account description and balance
            //      2. link to companion if applicable
            //      3. show transactions button (to new screen?  if so, viewModel could be scoped to the grandparent
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = account.name,
                    modifier = Modifier.weight(0.7f),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = account.balance.toPlainString(),
                    modifier = Modifier.weight(0.3f),
                    textAlign = TextAlign.Right,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Text(
                text = account.description,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onAction(AccountDetailAction.OnViewTransactionsClick) },
                ) {
                    Text(text = "View Transactions")
                }
                // TODO do I want a companion ID from the API pointing both ways?
                if (account.companionId !== null) {
                    Spacer(modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = { onAction(AccountDetailAction.OnCompanionSelected(account)) },
                    ) {
                        Text(text = "Companion Account")
                    }
                }
            }
        }
    }
}
