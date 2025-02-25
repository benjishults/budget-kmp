package bps.budget.jdbc.test

import bps.budget.persistence.AccountDao
import bps.budget.persistence.AnalyticsDao
import bps.budget.persistence.TransactionDao
import bps.budget.persistence.UserBudgetDao
import bps.budget.persistence.jdbc.JdbcAccountDao
import bps.budget.persistence.jdbc.JdbcAnalyticsDao
import bps.budget.persistence.jdbc.JdbcTransactionDao
import bps.budget.persistence.jdbc.JdbcUserBudgetDao
import bps.jdbc.JdbcConfig
import bps.jdbc.JdbcConnectionProvider
import bps.jdbc.JdbcFixture
import bps.jdbc.test.JdbcTestFixture
import bps.jdbc.toJdbcConnectionProvider

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

                override val jdbcConnectionProvider: JdbcConnectionProvider =
                    jdbcConfig.toJdbcConnectionProvider()

                override val accountDao: AccountDao =
                    JdbcAccountDao(jdbcConnectionProvider)
                override val transactionDao: TransactionDao =
                    JdbcTransactionDao(jdbcConnectionProvider)
                override val userBudgetDao: UserBudgetDao =
                    JdbcUserBudgetDao(jdbcConnectionProvider)
                override val analyticsDao: AnalyticsDao =
                    JdbcAnalyticsDao(jdbcConnectionProvider)
            }
    }

}
