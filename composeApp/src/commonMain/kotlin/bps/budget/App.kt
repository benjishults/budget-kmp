package bps.budget

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        var timeAtLocation: String by remember { mutableStateOf("No location selected") }
        Column {
            Text(timeAtLocation)
            Button(onClick = { timeAtLocation = "13:30" }) {
                Text("Show Time At Location")
            }
        }
//
//        var showContent: Boolean by remember { mutableStateOf(false) }
//        val greeting: String = remember { Greeting().greet() }
//        Column(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalAlignment = Alignment.CenterHorizontally,
//        ) {
//            Text(
//                text = "Today's date is ${todaysDate()}",
//                modifier = Modifier.padding(20.dp),
//                fontSize = 24.sp,
//                textAlign = TextAlign.Center,
//            )
//            Button(onClick = { showContent = !showContent }) {
//                Text("Click me!")
//            }
//            AnimatedVisibility(visible = showContent) {
//                Column(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                ) {
//                    Image(
//                        painter = painterResource(resource = Res.drawable.compose_multiplatform),
//                        contentDescription = null,
//                    )
//                    Text("Compose: $greeting")
//                }
//            }
//        }
    }
}

fun todaysDate(): String {
    fun LocalDateTime.format() = toString().substringBefore('T')

    val now = Clock.System.now()
    val zone = TimeZone.currentSystemDefault()
    return now.toLocalDateTime(zone).format()
}
