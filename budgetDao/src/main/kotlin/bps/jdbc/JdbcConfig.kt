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

data class HikariYamlConfig(
    val dataSourceClassName: String = "org.postgresql.ds.PGSimpleDataSource",
    val poolName: String = "postgres",
    val cachePrepStmts: Boolean = true,
    val prepStmtCacheSize: Int = 250,
    val prepStmtCacheSqlLimit: Int = 2048,
) {
    val autoCommit: Boolean = false
}
