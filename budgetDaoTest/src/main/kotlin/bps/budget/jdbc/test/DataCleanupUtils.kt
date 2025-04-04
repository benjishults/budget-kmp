@file:JvmName("DataCleanupUtils")
@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.jdbc.test

import bps.jdbc.JdbcFixture
import bps.jdbc.JdbcFixture.Companion.setUuid
import bps.jdbc.JdbcFixture.Companion.transactOrThrow
import java.math.BigDecimal
import java.sql.Connection
import javax.sql.DataSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

fun dropTables(dataSource: DataSource, schema: String) {
    require(schema == "clean_after_test")
    dataSource.transactOrThrow {
        createStatement()
            .use { statement ->
                statement.execute("drop table if exists transaction_items")
                statement.execute("drop table if exists transactions")
                statement.execute("drop table if exists account_active_periods")
                statement.execute("drop table if exists account_active_periods_temp")
                statement.execute("drop table if exists staged_draft_accounts")
                statement.execute("drop table if exists staged_real_accounts")
                statement.execute("drop table if exists staged_charge_accounts")
                statement.execute("drop table if exists staged_category_accounts")
                statement.execute("drop table if exists staged_accounts")
                statement.execute("drop table if exists accounts")
                statement.execute("drop table if exists draft_accounts")
                statement.execute("drop table if exists charge_accounts")
                statement.execute("drop table if exists checking_accounts")
                statement.execute("drop table if exists real_accounts")
                statement.execute("drop table if exists category_accounts")
                statement.execute("drop table if exists access_details")
                statement.execute("drop table if exists budget_access")
                statement.execute("drop type if exists coarse_access")
                statement.execute("drop type if exists fine_access")
                statement.execute("drop table if exists budgets")
                statement.execute("drop table if exists users")
                statement.execute("drop table if exists timestamps")
            }
    }
}

fun deleteAccounts(budgetId: Uuid, dataSource: DataSource): Int =
    with(JdbcFixture) {
        deleteTransactions(budgetId, dataSource)
        dataSource.transactOrThrow {
            prepareStatement("delete from account_active_periods where budget_id = ?")
                .use {
                    it.setUuid(1, budgetId)
                    it.executeUpdate()
                }
            prepareStatement("delete from accounts where budget_id = ?")
                .use {
                    it.setUuid(1, budgetId)
                    it.executeUpdate()
                }
        }
    }

fun deleteUser(userName: String, dataSource: DataSource): Unit =
    with(JdbcFixture) {
        dataSource.transactOrThrow {
            val listOfUserBudgets: List<Uuid> =
                buildList {
                    prepareStatement(
                        """
                        |select * from budget_access ba
                        |join users u on u.id = ba.user_id
                        |where u.login = ?
                        """
                            .trimMargin(),
                    )
                        .use {
                            it.setString(1, userName)
                            it.executeQuery()
                                .use {
                                    while (it.next()) {
                                        add(it.getUuid("budget_id")!!)
                                    }
                                }
                        }
                }
            listOfUserBudgets
                .forEach { budgetId ->
                    deleteAccounts(budgetId, dataSource)
                    prepareStatement("delete from budget_access where budget_id = ?")
                        .use {
                            it.setUuid(1, budgetId)
                            it.executeUpdate()
                        }
                }
            prepareStatement("delete from users where login = ?")
                .use {
                    it.setString(1, userName)
                    it.executeUpdate()
                }
        }
    }

fun deleteTransactions(budgetId: Uuid, dataSource: DataSource): Int =
    with(JdbcFixture) {
        dataSource.transactOrThrow {
            zeroBalance(budgetId)
            prepareStatement("delete from transaction_items where budget_id = ?")
                .use {
                    it.setUuid(1, budgetId)
                    it.executeUpdate()
                }
            prepareStatement("delete from transactions where budget_id = ?")
                .use {
                    it.setUuid(1, budgetId)
                    it.executeUpdate()
                }
        }
    }

private fun Connection.zeroBalance(budgetId: Uuid) {
    prepareStatement("update accounts set balance = ? where budget_id = ?")
        .use {
            it.setBigDecimal(1, BigDecimal.ZERO.setScale(2))
            it.setUuid(2, budgetId)
            it.executeUpdate()
        }
}
