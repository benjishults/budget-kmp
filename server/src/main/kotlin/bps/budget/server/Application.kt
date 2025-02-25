package bps.budget.server

import bps.budget.persistence.jdbc.JdbcAccountDao
import bps.budget.persistence.jdbc.JdbcAnalyticsDao
import bps.budget.persistence.jdbc.JdbcTransactionDao
import bps.budget.persistence.jdbc.JdbcUserBudgetDao
import bps.budget.server.account.accountRoutes
import bps.jdbc.JdbcConnectionProvider
import bps.config.convertToPath
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    val configurations =
        BudgetServerConfigurations(
            sequenceOf(
                "budget-server.yml",
                convertToPath("~/.config/bps-budget-server/budget-server.yml"),
            ),
        )
    val jdbcConnectionProvider = JdbcConnectionProvider(
        database = configurations.jdbc.database,
        dbProvider = configurations.jdbc.dbProvider,
        host = configurations.jdbc.host,
        port = configurations.jdbc.port,
        schema = configurations.jdbc.schema,
        user = configurations.jdbc.user,
        password = configurations.jdbc.password,
    )
    val accountDao = JdbcAccountDao(jdbcConnectionProvider.connection)
    val transactionDao = JdbcTransactionDao(jdbcConnectionProvider.connection, accountDao)
    val userBudgetDao = JdbcUserBudgetDao(jdbcConnectionProvider.connection)
    val analyticsDao = JdbcAnalyticsDao(jdbcConnectionProvider.connection, accountDao)

    embeddedServer(
        factory = Netty,
        port = 8081,
        host = "0.0.0.0",
    ) {
        module(accountDao)
    }
        .start(wait = true)
}

fun Application.module(accountDao: JdbcAccountDao) =
    routing {
        staticResources("/content", "static")
        accountRoutes(accountDao)
        get("/test") {
            call.respondText("succeeded")
        }
        get("/") {
            call.respondText("root")
        }
    }
