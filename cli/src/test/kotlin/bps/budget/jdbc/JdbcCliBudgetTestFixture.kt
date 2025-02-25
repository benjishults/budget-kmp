package bps.budget.jdbc

import bps.budget.BudgetConfigurations
import bps.budget.CliBudgetDao
import bps.budget.InitializingBudgetDao
import bps.budget.JdbcCliBudgetDao
import bps.budget.JdbcInitializingBudgetDao
import bps.budget.persistence.AccountDao
import bps.budget.persistence.AnalyticsDao
import bps.budget.persistence.TransactionDao
import bps.budget.persistence.UserBudgetDao
import bps.budget.persistence.jdbc.JdbcAccountDao
import bps.budget.persistence.jdbc.JdbcAnalyticsDao
import bps.jdbc.JdbcConfig
import bps.jdbc.JdbcConnectionProvider
import bps.budget.persistence.jdbc.JdbcTransactionDao
import bps.budget.persistence.jdbc.JdbcUserBudgetDao
import bps.jdbc.toJdbcConnectionProvider
import bps.jdbc.JdbcFixture

interface JdbcCliBudgetTestFixture : JdbcFixture, JdbcTestFixture {

    val budgetConfigurations: BudgetConfigurations
    val cliBudgetDao: CliBudgetDao
    val initializingBudgetDao: InitializingBudgetDao
    val accountDao: AccountDao
    val transactionDao: TransactionDao
    val userBudgetDao: UserBudgetDao
    val analyticsDao: AnalyticsDao

    companion object {
        operator fun invoke(
            budgetConfigurations: BudgetConfigurations,
        ): JdbcCliBudgetTestFixture =
            object : JdbcCliBudgetTestFixture {
                override val budgetConfigurations: BudgetConfigurations =
                    budgetConfigurations

                override val jdbcConfig: JdbcConfig =
                    budgetConfigurations.persistence.jdbc!!

                override val jdbcConnectionProvider: JdbcConnectionProvider =
                    jdbcConfig.toJdbcConnectionProvider()

                override val initializingBudgetDao: JdbcInitializingBudgetDao =
                    JdbcInitializingBudgetDao(
                        budgetName = budgetConfigurations.budget.name,
                        connectionProvider = jdbcConnectionProvider,
                    )
                override val cliBudgetDao: CliBudgetDao =
                    JdbcCliBudgetDao(
                        budgetName = budgetConfigurations.budget.name,
                        connectionProvider = jdbcConnectionProvider,
                    )
                override val accountDao: AccountDao =
                    JdbcAccountDao(jdbcConnectionProvider)
                override val transactionDao: TransactionDao =
                    JdbcTransactionDao(jdbcConnectionProvider)
                override val userBudgetDao: UserBudgetDao =
                    JdbcUserBudgetDao(jdbcConnectionProvider)
                override val analyticsDao: AnalyticsDao =
                    JdbcAnalyticsDao(jdbcConnectionProvider)
            }

        operator fun invoke(
            configFileName: String,
        ): JdbcCliBudgetTestFixture =
            invoke(BudgetConfigurations(sequenceOf(configFileName)))
    }

}
