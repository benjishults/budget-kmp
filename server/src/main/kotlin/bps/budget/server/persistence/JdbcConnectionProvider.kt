package bps.budget.server.persistence

import java.net.URLEncoder
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class JdbcConnectionProvider(
    database: String,
    dbProvider: String = "postgresql",
    host: String = "127.0.0.1",
    port: Int = 5432,
    schema: String? = null,
    user: String? = null,
    password: String? = null,
) : AutoCloseable {

    private var jdbcURL: String =
        "jdbc:${dbProvider}://${host}:${port}/${
            URLEncoder.encode(
                database,
                "utf-8",
            )
        }${
            if (schema === null) {
                ""
            } else {
                "?currentSchema=${URLEncoder.encode(schema, "utf-8")}"
            }
        }"

    var connection: Connection = startConnection(user, password)
        private set
    private val keepAliveSingleThreadScheduledExecutor = Executors
        .newSingleThreadScheduledExecutor()

    init {
        // NOTE keep the connection alive with an occasional call to `isValid`.
        keepAliveSingleThreadScheduledExecutor
            .apply {
                scheduleWithFixedDelay(
                    {
                        if (!connection.isValid(4_000)) {
                            // TODO log this
                            connection = startConnection(user, password)
                        }
                    },
                    5_000,
                    20_000,
                    TimeUnit.SECONDS,
                )
            }
    }

    private fun startConnection(user: String?, password: String?): Connection =
        DriverManager
            .getConnection(
                jdbcURL,
                user ?: System.getenv("BUDGET_JDBC_USER"),
                password ?: System.getenv("BUDGET_JDBC_PASSWORD"),
            )
            .apply {
                autoCommit = false
            }

    override fun close() {
        keepAliveSingleThreadScheduledExecutor.close()
        connection.close()
    }

}
