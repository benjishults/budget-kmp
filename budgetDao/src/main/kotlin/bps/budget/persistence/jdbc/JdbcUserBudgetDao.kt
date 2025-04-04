package bps.budget.persistence.jdbc

import bps.budget.model.AuthenticatedUser
import bps.budget.model.CoarseAccess
import bps.budget.model.User
import bps.budget.model.UserBudgetAccess
import bps.budget.persistence.BudgetEntity
import bps.budget.persistence.UserBudgetDao
import bps.budget.persistence.UserEntity
import bps.jdbc.JdbcFixture
import bps.jdbc.JdbcFixture.Companion.transactOrThrow
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class JdbcUserBudgetDao(
    val dataSource: DataSource,
) : UserBudgetDao, JdbcFixture {

    override fun getUserByLoginOrNull(login: String): User? =
        dataSource.transactOrThrow {
            prepareStatement(
                """
                |select *
                |from users u
                |  left join budget_access ba on u.id = ba.user_id
                |where u.login = ?
                """.trimMargin(),
            )
                .use { preparedStatement: PreparedStatement ->
                    preparedStatement.setString(1, login)
                    preparedStatement.executeQuery()
                        .use { resultSet: ResultSet ->
                            if (resultSet.next()) {
                                val budgets = mutableListOf<UserBudgetAccess>()
                                val id: Uuid = resultSet.getUuid("id")!!
                                do {
                                    val budgetName: String? = resultSet.getString("budget_name")
                                    if (budgetName !== null)
                                        budgets.add(
                                            UserBudgetAccess(
                                                budgetId = resultSet.getUuid("budget_id")!!,
                                                budgetName = budgetName,
                                                timeZone = resultSet.getString("time_zone")
                                                    ?.let { timeZone -> TimeZone.of(timeZone) }
                                                    ?: TimeZone.currentSystemDefault(),
                                                analyticsStart = resultSet.getInstant("analytics_start"),
                                                coarseAccess = resultSet.getString("coarse_access")
                                                    ?.let(CoarseAccess::valueOf),
                                            ),
                                        )
                                } while (resultSet.next())
                                // NOTE not doing authentication yet.
                                AuthenticatedUser(id, login, budgets)
                            } else
                                null
                        }
                }
        }

    override fun createUser(login: String, password: String): UserEntity =
        UserEntity(
            Uuid
                .random()
                .also {
                    dataSource.transactOrThrow {
                        prepareStatement("insert into users (login, id) values (?, ?)")
                            .use { statement ->
                                statement.setString(1, login)
                                statement.setUuid(2, it)
                                statement.executeUpdate()
                            }
                    }
                },
            login,
        )

    override fun grantAccess(
        budgetName: String,
        timeZoneId: String,
        analyticsStart: Instant,
        userId: Uuid,
        budgetId: Uuid,
    ) {
        dataSource.transactOrThrow {
            prepareStatement(
                """
                    insert into budget_access (id, budget_id, user_id, time_zone, analytics_start, budget_name)
                    values (?, ?, ?, ?, ?, ?) on conflict do nothing
                """.trimIndent(),
            )
                .use { createBudgetStatement: PreparedStatement ->
                    createBudgetStatement.setUuid(1, Uuid.random())
                    createBudgetStatement.setUuid(2, budgetId)
                    createBudgetStatement.setUuid(3, userId)
                    createBudgetStatement.setString(4, timeZoneId)
                    createBudgetStatement.setInstant(5, analyticsStart)
                    createBudgetStatement.setString(6, budgetName)
                    createBudgetStatement.executeUpdate()
                }
        }
    }

    override fun updateTimeZone(timeZoneId: String, userId: Uuid, budgetId: Uuid): Unit =
        dataSource.transactOrThrow {
            prepareStatement(
                """
                    update budget_access
                    set time_zone = ?
                    where user_id = ?
                      and budget_id = ?
                """.trimIndent(),
            )
                .use { updateTimeZoneStatement: PreparedStatement ->
                    updateTimeZoneStatement.setString(1, timeZoneId)
                    updateTimeZoneStatement.setUuid(2, userId)
                    updateTimeZoneStatement.setUuid(3, budgetId)
                    if (updateTimeZoneStatement.executeUpdate() != 1)
                        throw IllegalArgumentException("budget access not found userId=$userId, budgetId=$budgetId")
                }
        }

    override fun updateAnalyticsStart(analyticsStart: Instant, userId: Uuid, budgetId: Uuid): Int =
        dataSource.transactOrThrow {
            prepareStatement(
                """
                    update budget_access
                    set analytics_start = ?
                    where user_id = ?
                      and budget_id = ?
                """.trimIndent(),
            )
                .use { updateTimeZoneStatement: PreparedStatement ->
                    updateTimeZoneStatement.setInstant(1, analyticsStart)
                    updateTimeZoneStatement.setUuid(2, userId)
                    updateTimeZoneStatement.setUuid(3, budgetId)
                    updateTimeZoneStatement.executeUpdate()
                }
        }

    override fun createBudget(generalAccountName: String): BudgetEntity =
        dataSource.transactOrThrow {
            require(generalAccountName.isNotBlank())
            prepareStatement(
                """
                insert into budgets (id, general_account_name)
                values (?, ?) on conflict do nothing""",
            )
                .use { createBudgetStatement: PreparedStatement ->
                    val budgetId = Uuid.random()
                    createBudgetStatement.setUuid(1, budgetId)
                    createBudgetStatement.setString(2, generalAccountName)
                    createBudgetStatement.executeUpdate()
                    BudgetEntity(
                        budgetId,
                        generalAccountName,
                    )
                }
        }

    override fun deleteUser(userId: Uuid) {
        dataSource.transactOrThrow {
            prepareStatement("delete from budget_access where user_id = ?")
                .use { statement: PreparedStatement ->
                    statement.setUuid(1, userId)
                    if (statement.executeUpdate() != 1)
                        throw IllegalStateException("Failed to delete user userId=$userId")
                }
            prepareStatement("delete from users where id = ?")
                .use { statement: PreparedStatement ->
                    statement.setUuid(1, userId)
                    if (statement.executeUpdate() != 1)
                        throw IllegalStateException("Failed to delete user userId=$userId")
                }
        }
    }


    override fun deleteUserByLogin(login: String) {
//        errorStateTracker.catchCommitErrorState {
        dataSource.transactOrThrow {
            getUserIdByLogin(login)
                ?.let { userId: Uuid ->
                    prepareStatement("delete from budget_access where user_id = ?")
                        .use { statement: PreparedStatement ->
                            statement.setUuid(1, userId)
                            if (statement.executeUpdate() != 1)
                                throw IllegalStateException("User not found: userId=$userId, login=$login")
                        }
                    prepareStatement("delete from users where login = ?")
                        .use { statement: PreparedStatement ->
                            statement.setString(1, login)
                            if (statement.executeUpdate() != 1)
                                throw IllegalStateException("User not found: userId=$userId, login=$login")
                        }
                }
        }
    }

    private fun Connection.getUserIdByLogin(login: String): Uuid? =
        prepareStatement("select id from users where login = ?")
            .use { statement: PreparedStatement ->
                statement.setString(1, login)
                statement.executeQuery()
                    .use { resultSet: ResultSet ->
                        if (resultSet.next()) {
                            resultSet.getUuid("id")
                        } else
                            null
                    }
            }

    override fun deleteBudget(budgetId: Uuid) {
        dataSource.transactOrThrow {
            prepareStatement("delete from budget_access where budget_id = ?")
                .use { statement: PreparedStatement ->
                    statement.setUuid(1, budgetId)
                    if (statement.executeUpdate() != 1)
                        throw IllegalStateException("Budget not found: budgetId=$budgetId")
                }
            prepareStatement("delete from budgets where id = ?")
                .use { statement: PreparedStatement ->
                    statement.setUuid(1, budgetId)
                    if (statement.executeUpdate() != 1)
                        throw IllegalStateException("Budget not found: budgetId=$budgetId")
                }
        }
    }

}
