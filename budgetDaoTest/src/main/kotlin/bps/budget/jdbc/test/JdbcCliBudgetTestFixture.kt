package bps.budget.jdbc.test

import bps.budget.persistence.AccountDao
import bps.budget.persistence.AnalyticsDao
import bps.budget.persistence.TransactionDao
import bps.budget.persistence.UserBudgetDao
import bps.budget.persistence.jdbc.JdbcAccountDao
import bps.budget.persistence.jdbc.JdbcAnalyticsDao
import bps.budget.persistence.jdbc.JdbcTransactionDao
import bps.budget.persistence.jdbc.JdbcUserBudgetDao
import bps.jdbc.HikariYamlConfig
import bps.jdbc.JdbcConfig
import bps.jdbc.JdbcFixture
import bps.jdbc.configureDataSource
import bps.jdbc.test.JdbcTestFixture
import javax.sql.DataSource

interface JdbcCliBudgetTestFixture : JdbcFixture, JdbcTestFixture {

    val budgetName: String
    val accountDao: AccountDao
    val transactionDao: TransactionDao
    val userBudgetDao: UserBudgetDao
    val analyticsDao: AnalyticsDao

    companion object {
        operator fun invoke(
            jdbcConfig: JdbcConfig,
            budgetName: String,
        ): JdbcCliBudgetTestFixture =
            object : JdbcCliBudgetTestFixture {

                override val budgetName: String = budgetName
                override val jdbcConfig: JdbcConfig = jdbcConfig

                override val dataSource: DataSource = configureDataSource(jdbcConfig, HikariYamlConfig())

                override val accountDao: AccountDao =
                    JdbcAccountDao(dataSource)
                override val transactionDao: TransactionDao =
                    JdbcTransactionDao(dataSource)
                override val userBudgetDao: UserBudgetDao =
                    JdbcUserBudgetDao(dataSource)
                override val analyticsDao: AnalyticsDao =
                    JdbcAnalyticsDao(dataSource)
            }
    }

}
