package bps.jdbc

import java.net.URLEncoder
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class JdbcConnectionProvider(
    database: String,
    dbProvider: String = "postgresql",
    host: String = "127.0.0.1",
    port: Int = 5432,
    schema: String? = null,
    user: String? = null,
    password: String? = null,
) : AutoCloseable {

    private var closed = AtomicReference<Boolean>(false)

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
                    /* command = */
                    {
                        if (!connection.isValid(4)) {
                            // TODO log this
                            connection = startConnection(user, password)
                        }
                    },
                    /* initialDelay = */ 5,
                    /* delay = */ 20,
                    /* unit = */ TimeUnit.SECONDS,
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

    /**
     * It is safe to call this multiple times.  On first call, this stops the keep-alive process and closes the connection.
     */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            keepAliveSingleThreadScheduledExecutor.close()
            connection.close()
        }
    }

}
