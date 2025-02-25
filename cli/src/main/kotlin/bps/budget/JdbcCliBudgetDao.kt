package bps.budget

import bps.budget.model.Account
import bps.budget.model.AccountType
import bps.budget.model.BudgetData
import bps.budget.model.CategoryAccount
import bps.budget.model.ChargeAccount
import bps.budget.model.DraftAccount
import bps.budget.model.RealAccount
import bps.budget.persistence.AccountDao
import bps.budget.persistence.DataConfigurationException
import bps.jdbc.JdbcConnectionProvider
import bps.jdbc.JdbcFixture
import bps.jdbc.JdbcFixture.Companion.transact
import bps.kotlin.Instrumentable
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.UUID

// TODO this class is doing too much... could be split up... See how it's done in server.
@Instrumentable
class JdbcCliBudgetDao(
    val budgetName: String,
    private val connectionProvider: JdbcConnectionProvider,
) : CliBudgetDao, JdbcFixture {

    private val connection: Connection = connectionProvider.connection

    private data class BudgetDataInfo(
        val generalAccountId: UUID,
        val timeZone: TimeZone,
        val analyticsStart: Instant,
        val budgetName: String,
    )

    /**
     * Just loads top-level account info.  Details of transactions are loaded on-demand.
     * @throws DataConfigurationException if data isn't found.
     */
    override fun load(budgetId: UUID, userId: UUID, accountDao: AccountDao): BudgetData =
        try {
            connection.transact(
                onRollback = { ex ->
                    throw DataConfigurationException(ex.message, ex)
                },
            ) {
                val (generalAccountId: UUID, timeZone: TimeZone, analyticsStart: Instant, budgetName: String) =
                    prepareStatement(
                        """
                            select b.general_account_id, ba.time_zone, ba.budget_name, ba.analytics_start
                            from budgets b
                                join budget_access ba on b.id = ba.budget_id
                                join users u on u.id = ba.user_id
                            where b.id = ?
                                and u.id = ?
                        """.trimIndent(),
                    )
                        .use { getBudget: PreparedStatement ->
                            getBudget.setUuid(1, budgetId)
                            getBudget.setUuid(2, userId)
                            getBudget.executeQuery()
                                .use { result: ResultSet ->
                                    if (result.next()) {
                                        BudgetDataInfo(
                                            generalAccountId = result.getObject("general_account_id", UUID::class.java),
                                            timeZone = result.getString("time_zone")
                                                ?.let { timeZone -> TimeZone.of(timeZone) }
                                                ?: TimeZone.currentSystemDefault(),
                                            analyticsStart = result.getInstant("analytics_start"),
                                            budgetName = result.getString("budget_name"),
                                        )
                                    } else
                                        throw DataConfigurationException("Budget data not found for name: $budgetName")
                                }
                        }
                // TODO pull out duplicate code in these next three sections
                val categoryAccounts: List<CategoryAccount> =
                    accountDao.getActiveAccounts(AccountType.category.name, budgetId, ::CategoryAccount)
                val generalAccount: CategoryAccount =
                    categoryAccounts.find {
                        it.id == generalAccountId
                    }!!
                val realAccounts: List<RealAccount> =
                    accountDao.getActiveAccounts(AccountType.real.name, budgetId, ::RealAccount)
                val chargeAccounts: List<ChargeAccount> =
                    accountDao.getActiveAccounts(AccountType.charge.name, budgetId, ::ChargeAccount)
                val draftAccounts: List<DraftAccount> = // getAccounts("draft", budgetId, ::DraftAccount)
                    prepareStatement(
                        """
select acc.*
from accounts acc
         join account_active_periods aap
              on acc.id = aap.account_id
                  and acc.budget_id = aap.budget_id
where acc.budget_id = ?
  and acc.type = ?
  and now() > aap.start_date_utc
  and now() < aap.end_date_utc
""".trimIndent(),
                    )
                        .use { getDraftAccountsStatement ->
                            getDraftAccountsStatement.setUuid(1, budgetId)
                            getDraftAccountsStatement.setString(2, AccountType.draft.name)
                            getDraftAccountsStatement.executeQuery()
                                .use { result ->
                                    buildList {
                                        while (result.next()) {
                                            add(
                                                DraftAccount(
                                                    result.getString("name"),
                                                    result.getString("description"),
                                                    result.getObject("id", UUID::class.java),
                                                    result.getCurrencyAmount("balance"),
                                                    realAccounts.find {
                                                        it.id.toString() == result.getString("companion_account_id")
                                                    }!!,
                                                    budgetId,
                                                ),
                                            )
                                        }
                                    }
                                }
                        }
                BudgetData(
                    budgetId,
                    budgetName,
                    timeZone,
                    analyticsStart,
                    generalAccount,
                    categoryAccounts,
                    realAccounts,
                    chargeAccounts,
                    draftAccounts,
                )
            }
        } catch (ex: Exception) {
            if (ex is DataConfigurationException) {
                throw ex
            } else
                throw DataConfigurationException(ex)
        }

    /**
     * Must be called within a transaction with manual commits
     */
    // TODO we want to be in a state where we don't need to call this!
    private fun Connection.upsertAccountData(
        accounts: List<Account>,
        accountType: String,
        budgetId: UUID,
    ) {
        accounts.forEach { account ->
            // upsert account
            prepareStatement(
                """
                insert into staged_accounts (id, name, description, balance,${if (accountType == "draft") "companion_account_id, " else ""} budget_id)
                VALUES (?, ?, ?, ?,${if (accountType == "draft") " ?," else ""} ?)
                on conflict do nothing
            """.trimIndent(),
            )
                .use { createStagedAccountStatement: PreparedStatement ->
                    var parameterIndex = 1
                    createStagedAccountStatement.setUuid(parameterIndex++, account.id)
                    createStagedAccountStatement.setString(parameterIndex++, account.name)
                    createStagedAccountStatement.setString(parameterIndex++, account.description)
                    createStagedAccountStatement.setBigDecimal(parameterIndex++, account.balance)
                    if (accountType == "draft") {
                        createStagedAccountStatement.setUuid(
                            parameterIndex++,
                            (account as DraftAccount).realCompanion.id,
                        )
                    }
                    createStagedAccountStatement.setUuid(parameterIndex++, budgetId)
                    createStagedAccountStatement.executeUpdate()
                }
            prepareStatement(
                """
                merge into accounts as t
                    using staged_accounts as s
                    on (t.id = s.id or t.name = s.name) and t.budget_id = s.budget_id
                    when matched then
                        update
                        set name = s.name,
                            description = s.description,
                            balance = s.balance
                    when not matched then
                        insert (id, name, description, balance, type, ${if (accountType == "draft") "companion_account_id, " else ""} budget_id)
                        values (s.id, s.name, s.description, s.balance, ?, ${if (accountType == "draft") "s.companion_account_id, " else ""} s.budget_id);
                """.trimIndent(),
            )
                .use { createAccountStatement: PreparedStatement ->
                    createAccountStatement.setString(1, accountType)
                    createAccountStatement.executeUpdate()
                }
            // upsert account_active_periods entry
            prepareStatement(
                """
                    insert into account_active_periods (id, account_id, budget_id)
                    values (?, ?, ?)
                    on conflict do nothing
                """.trimIndent(),
            )
                .use { createActivePeriod: PreparedStatement ->
                    createActivePeriod.setUuid(1, UUID.randomUUID())
                    createActivePeriod.setUuid(2, account.id)
                    createActivePeriod.setUuid(3, budgetId)
                    // NOTE due to the uniqueness constraints on this table, this will be idempotent
                    createActivePeriod.executeUpdate()
                }
        }
    }

    override fun close() {
        super.close()
        connectionProvider.close()
    }

}
