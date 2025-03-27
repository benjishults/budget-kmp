package bps.budget.server

import bps.budget.persistence.AccountDao
import bps.budget.persistence.TransactionDao
import bps.budget.persistence.jdbc.JdbcAccountDao
import bps.budget.persistence.jdbc.JdbcAnalyticsDao
import bps.budget.persistence.jdbc.JdbcTransactionDao
import bps.budget.persistence.jdbc.JdbcUserBudgetDao
import bps.budget.server.account.accountRoutes
import bps.budget.server.transaction.transactionRoutes
import bps.config.convertToPath
import bps.jdbc.configureDataSource
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

fun main() {
    val configurations =
        BudgetServerConfigurations(
            sequenceOf(
                "budget-server.yml",
                convertToPath("~/.config/bps-budget-server/budget-server.yml"),
            ),
        )
    val dataSource = configureDataSource(configurations.jdbc, configurations.hikari)

    val accountDao = JdbcAccountDao(dataSource)
    val transactionDao = JdbcTransactionDao(dataSource)
    val userBudgetDao = JdbcUserBudgetDao(dataSource)
    val analyticsDao = JdbcAnalyticsDao(dataSource, Clock.System)

    embeddedServer(
        factory = Netty,
        port = configurations.server.port,
        host = "0.0.0.0",
    ) {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                },
            )
        }
        module(accountDao, transactionDao)
    }
        .start(wait = true)
}

fun Application.module(
    accountDao: AccountDao,
    transactionDao: TransactionDao,
) =
    routing {
        staticResources("/content", "static")
        accountRoutes(accountDao)
        transactionRoutes(transactionDao)
        get("/test") {
            call.respondText("succeeded")
        }
        get("/") {
            call.respondText("root")
        }
    }
