package bps.budget

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import bps.budget.app.App
import bps.budget.di.productionAccountBalancesViewModel

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
//    initKoin()
    ComposeViewport(viewportContainerId = "composeApplication") {
        App(productionAccountBalancesViewModel)
    }
}
