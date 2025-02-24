package bps.budget.persistence.jdbc

data class JdbcConfig(
    val database: String = "budget",
    val schema: String = "public",
    val dbProvider: String = "postgresql",
    val port: Int = 5432,
    val host: String = "localhost",
    val user: String? = null,
    val password: String? = null,
)
