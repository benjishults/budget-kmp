package bps.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App(/*mainViewModel: MainViewModel = viewModel()*/) {
    MaterialTheme {
        val greetings: List<String> = listOf("hi", "hello")//by mainViewModel.greetingList.collectAsStateWithLifecycle()

        Column(
            modifier = Modifier.padding(all = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            greetings
                .forEach { greeting: String ->
                    Text(greeting)
                    Divider()
                }
        }
    }
}
