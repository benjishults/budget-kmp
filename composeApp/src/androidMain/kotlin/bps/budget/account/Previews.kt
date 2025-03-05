package bps.budget.account

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.tooling.preview.Preview
import bps.budget.account.presentation.balances.components.MultiSelectView
import bps.budget.account.presentation.balances.components.SelectableItemModel
import bps.budget.model.AccountType

@Composable
@Preview
fun MultiSelectPreview() {
    MaterialTheme {
        MultiSelectView(list = mutableStateListOf<SelectableItemModel>(*AccountType.entries.toList().map { SelectableItemModel(it.name, mutableStateOf(false)) }.toTypedArray()))
    }
}
