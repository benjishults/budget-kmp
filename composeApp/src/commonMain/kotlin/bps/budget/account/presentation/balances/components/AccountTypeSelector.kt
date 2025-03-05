package bps.budget.account.presentation.balances.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import bps.budget.model.AccountType

@Composable
fun AccountTypeSelector(
    selectedTypes: List<AccountType>,
    onTypesChange: (List<AccountType>) -> Unit,
    onImeSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
//    val selectedTypes: SnapshotStateList<AccountType?> = remember { mutableStateListOf<AccountType?>() }
//    val resetSelectionMode = {
//        isInSelectionMode = false
//        selectedItems.clear()
//    }
//    LazyColumn(modifier) {
//        items(AccountType.entries.toList()) { type: AccountType ->
//            ListItem(
//                modifier = Modifier.combinedClickable(
//                    onClick = {
//                        // Click action if required when not in selection mode
//                    },
//                    leadingContent = { Icon(imageVector =, contentDescription =) },
//                    headlineContent = { Text(text = type.name) },
//                ),
//                text = { Text(type.name) },
//            )
//        }
//    }
}

@Composable
fun MultiSelectView(list: MutableList<SelectableItemModel>) {
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = Color.White,
    ) {
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(items = list) { item ->
                MultiSelectItemView(model = item)
            }
        }
    }
}

@Composable
fun MultiSelectItemView(model: SelectableItemModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentHeight(),
        // TODO padding
    ) {
        Text(
            text = model.text,
            fontSize =
                if (model.selected.value)
                    30.sp
                else
                    18.sp,
            color =
                if (model.selected.value)
                    Color.Black
                else
                    Color.Gray,
            fontWeight =
                if (model.selected.value)
                    FontWeight.Bold
                else
                    FontWeight.Normal,
            modifier = Modifier
                .align(Alignment.CenterStart)
        )
        Checkbox(
            checked = model.selected.value,
            onCheckedChange = {
                model.toggle()
            },
            colors =
                CheckboxDefaults.colors(
                    checkedColor = Color.Black,
                    uncheckedColor = Color.Gray
                ),
            modifier = Modifier
                .align(Alignment.CenterEnd)
        )

    }
}

data class SelectableItemModel(
    val text: String,
    val selected: MutableState<Boolean>,
) {
    fun toggle() {
        selected.value = !selected.value
    }
}
