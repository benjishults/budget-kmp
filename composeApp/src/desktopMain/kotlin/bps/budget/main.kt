package bps.budget

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import bps.budget.di.productionAccountBalancesViewModel

fun main() {
//    initKoin()
    return application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "BPS Budget",
        ) {
            App(productionAccountBalancesViewModel)
        }
    }
}
