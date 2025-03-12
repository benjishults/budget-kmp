package bps.budget.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual val productionEngine: HttpClientEngine = OkHttp.create()
