package bps.jdbc.test

import bps.jdbc.JdbcConfig
import bps.jdbc.JdbcConnectionProvider
import bps.jdbc.JdbcFixture
import io.kotest.core.spec.Spec
import java.sql.Connection

interface JdbcTestFixture : JdbcFixture {

    val jdbcConfig: JdbcConfig
    val jdbcConnectionProvider: JdbcConnectionProvider
    val connection: Connection
        get() = jdbcConnectionProvider.connection

    fun Spec.closeJdbcAfterSpec() {
        afterSpec {
            jdbcConnectionProvider.close()
        }
    }

//    companion object {
////        operator fun invoke(fileName: String) =
//        operator fun invoke(fileName: String) =
//            object : JdbcTestFixture {
//                override val jdbcConfig: JdbcConfig
//                    get() = TODO("Not yet implemented")
//                override val jdbcConnectionProvider: JdbcConnectionProvider = jdbcConfig.toJdbcConnectionProvider()
//            }
//    }

}
