package bps.jdbc

data class JdbcConfig(
    val database: String = "budget",
    val schema: String = "public",
    val dbProvider: String = "postgresql",
    val port: Int = 5432,
    val host: String = "localhost",
    val user: String? = null,
    val password: String? = null,
)

fun JdbcConfig.toJdbcConnectionProvider() =
    JdbcConnectionProvider(
        database = database,
        dbProvider = dbProvider,
        host = host,
        port = port,
        schema = schema,
        user = user,
        password = password,
    )
