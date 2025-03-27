package bps.jdbc

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

fun configureDataSource(jdbc: JdbcConfig, hikari: HikariYamlConfig): HikariDataSource {
    val hikariConfig = HikariConfig()
    hikariConfig.dataSourceClassName = hikari.dataSourceClassName
    hikariConfig.username = jdbc.user
    hikariConfig.password = jdbc.password
//    hikariConfig.addDataSourceProperty("cachePrepStmts", hikari.cachePrepStmts)
//    hikariConfig.addDataSourceProperty("prepStmtCacheSize", hikari.prepStmtCacheSize)
//    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", hikari.prepStmtCacheSqlLimit)
    hikariConfig.addDataSourceProperty("databaseName", jdbc.database)
    hikariConfig.addDataSourceProperty("portNumber", jdbc.port)
    hikariConfig.addDataSourceProperty("serverName", jdbc.host)
    hikariConfig.isAutoCommit = hikari.autoCommit
    hikariConfig.poolName = hikari.poolName
    hikariConfig.schema = jdbc.schema
    val dataSource = HikariDataSource(hikariConfig)
    return dataSource
}
