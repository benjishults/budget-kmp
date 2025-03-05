package bps.budget.account

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
import bps.budget.account.presentation.balances.AccountBalancesScreen
import bps.budget.account.presentation.balances.AccountBalancesState
import bps.budget.account.presentation.balances.components.MultiSelectView
import bps.budget.account.presentation.balances.components.SelectableItemModel
import bps.budget.model.AccountResponse
import bps.budget.model.AccountType
import bps.kotlin.DecimalWithCents
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
@Preview
private fun MultiSelectPreview() {
    MaterialTheme {
        MultiSelectView(
            list = mutableStateListOf<SelectableItemModel>(
                *AccountType
                    .entries
                    .map { SelectableItemModel(it.name, mutableStateOf(false)) }
                    .toTypedArray(),
            ),
        )
    }
}

@OptIn(ExperimentalUuidApi::class)
@Preview
@Composable
private fun AccountBalancePreview() {
    AccountBalancesScreen(
        state = AccountBalancesState(
            accountTypes = listOf(AccountType.real),
            accounts = listOf(AccountResponse(
                name = "Savings",
                id = Uuid.random(),
                type = AccountType.real,
                balance = DecimalWithCents("25.09"),
                description = TODO(),
                budgetId = TODO(),
                companionId = TODO()
            )),
            isLoading = TODO(),
            selectedAccount = TODO(),
            errorMessage = TODO()
        )
    ) {  }
}
