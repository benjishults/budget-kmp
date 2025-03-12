package bps.budget.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

actual val productionEngine: HttpClientEngine = Js.create()
