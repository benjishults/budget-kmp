package bps.jdbc.test

import bps.jdbc.JdbcConfig
import bps.jdbc.JdbcFixture
import javax.sql.DataSource

interface JdbcTestFixture : JdbcFixture {

    val jdbcConfig: JdbcConfig
    val dataSource: DataSource

}
