package bps.budget.persistence.jdbc

import bps.budget.model.AccountType
import bps.budget.model.defaultGeneralAccountDescription
import bps.budget.model.defaultGeneralAccountName
import bps.budget.persistence.AccountDao
import bps.budget.persistence.AccountEntity
import bps.jdbc.JdbcFixture
import bps.jdbc.JdbcFixture.Companion.transactOrThrow
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
open class JdbcAccountDao(
    val dataSource: DataSource,
) : AccountDao, JdbcFixture {

    override fun getAllAccountNamesForBudget(budgetId: Uuid): List<String> =
        dataSource.transactOrThrow {
            buildList {
                AccountType.entries.forEach { type ->
                    addAll(
                        getActiveAccountsInternal(type.name, budgetId)
                            .map { it.name },
                    )
                    addAll(
                        getDeactivatedAccountsInternal(type.name, budgetId)
                            .map { it.name },
                    )
                }
            }
        }

    override fun getAccountOrNull(accountId: Uuid, budgetId: Uuid/*, transactional: Boolean*/): AccountEntity? =
        dataSource.transactOrThrow {
            this.getAccountOrNull(accountId, budgetId)
        }

     override fun Connection.getAccountOrNull(
        accountId: Uuid,
        budgetId: Uuid,
    ): AccountEntity? =
        prepareStatement(
            """
                |select name, type, description, balance, companion_account_id
                |from accounts
                |where id = ?
                |  and budget_id = ?
            """.trimMargin(),
        )
            .use { statement ->
                statement.setUuid(1, accountId)
                statement.setUuid(2, budgetId)
                statement.executeQuery()
                    .use { result ->
                        if (result.next()) {
                            val companionId: Uuid? = result.getUuid("companion_account_id")
                            val name = result.getString("name")!!
                            val description = result.getString("description")!!
                            val balance = result.getCurrencyAmount("balance")
                            AccountEntity(
                                name = name,
                                description = description,
                                id = accountId,
                                balance = balance,
                                budgetId = budgetId,
                                companionId = companionId,
                                type = result.getString("type")!!,
                            )
                        } else {
                            null
                        }
                    }
            }

    override fun getDeactivatedAccounts(type: String, budgetId: Uuid): List<AccountEntity> =
        dataSource.transactOrThrow {
            getDeactivatedAccountsInternal(type, budgetId)
        }

    private fun Connection.getDeactivatedAccountsInternal(type: String, budgetId: Uuid): List<AccountEntity> =
        prepareStatement(
            """
    select acc.*
    from accounts acc
    where acc.type = ?
        and acc.budget_id = ?
        and not exists
            (select 1
             from account_active_periods aap
             where acc.id = aap.account_id
                 and acc.budget_id = aap.budget_id
                 and aap.end_date_utc > now()
                 and aap.start_date_utc < now()
            )
""".trimIndent(),
        )
            .use { getDeactivatedAccountsStatement ->
                getDeactivatedAccountsStatement.setString(1, type)
                getDeactivatedAccountsStatement.setUuid(2, budgetId)
                getDeactivatedAccountsStatement
                    .executeQuery()
                    .use { resultSet: ResultSet ->
                        resultSet.extractAccounts(budgetId)
                    }
            }

    override fun getActiveAccounts(type: String, budgetId: Uuid): List<AccountEntity> =
        dataSource.transactOrThrow {
            getActiveAccountsInternal(type, budgetId)
        }

    private fun Connection.getActiveAccountsInternal(type: String, budgetId: Uuid): List<AccountEntity> =
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
            .use { getAccounts: PreparedStatement ->
                getAccounts.setUuid(1, budgetId)
                getAccounts.setString(2, type)
                getAccounts.executeQuery()
                    .use { result ->
                        result.extractAccounts(budgetId)
                    }
            }

    private fun ResultSet.extractAccounts(
        // TODO should be AccountFactory?
//        factory: (String, String, Uuid, BigDecimal, Uuid, Uuid?) -> T,
        budgetId: Uuid,
    ): List<AccountEntity> =
        buildList {
            while (next()) {
                add(
                    AccountEntity(
                        name = getString("name"),
                        description = getString("description"),
                        id = getUuid("id")!!,
                        balance = getCurrencyAmount("balance"),
                        budgetId = budgetId,
                        companionId = getUuid("companion_account_id"),
                        type = getString("type"),
                    ),
                )
            }
        }

    override fun deactivateAccount(accountId: Uuid): Unit =
        dataSource.transactOrThrow {
            prepareStatement(
                """
update account_active_periods aap
set end_date_utc = now()
where aap.account_id = ?
  and now() > aap.start_date_utc
  and now() < aap.end_date_utc
                """.trimIndent(),
            )
                .use { deactivateActivityPeriod: PreparedStatement ->
                    deactivateActivityPeriod.setUuid(1, accountId)
                    if (deactivateActivityPeriod.executeUpdate() != 1)
                        throw IllegalArgumentException("Could not deactivate accountId=$accountId")
                }
        }

    override fun List<AccountDao.AccountCommitableTransactionItem>.updateBalances(budgetId: Uuid): Unit =
        dataSource.transactOrThrow {
            updateBalances(this@updateBalances, budgetId)
        }

    override fun Connection.updateBalances(
        balancesToAdd: List<AccountDao.AccountCommitableTransactionItem>,
        budgetId: Uuid,
    ): Unit =
        balancesToAdd
            .forEach { item: AccountDao.AccountCommitableTransactionItem ->
                val amount: BigDecimal = item.amount
                val accountId: Uuid = item.accountId
                prepareStatement(
                    """
                        update accounts
                        set balance = balance + ?
                        where id = ? and budget_id = ?""".trimIndent(),
                )
                    .use { preparedStatement: PreparedStatement ->
                        preparedStatement.setBigDecimal(1, amount)
                        preparedStatement.setUuid(2, accountId)
                        preparedStatement.setUuid(3, budgetId)
                        if (preparedStatement.executeUpdate() != 1) {
                            throw IllegalStateException("Cannot update balances for account_id=$accountId, budget_id=$budgetId")
                        }
                    }
            }

    override fun updateAccount(
        id: Uuid,
        name: String,
        description: String,
        budgetId: Uuid,
    ): Boolean =
        dataSource.transactOrThrow {
            prepareStatement(
                """
                update accounts
                set name = ?,
                    description = ?
                where id = ?
                    and budget_id = ?
            """.trimIndent(),
            )
                .use { preparedStatement: PreparedStatement ->
                    preparedStatement.setString(1, name)
                    preparedStatement.setString(2, description)
                    preparedStatement.setUuid(3, id)
                    preparedStatement.setUuid(4, budgetId)
                    preparedStatement.executeUpdate() == 1
                }
        }

    override fun createAccount(
        name: String,
        description: String,
        type: String,
        balance: BigDecimal,
        budgetId: Uuid,
    ): AccountEntity =
        dataSource.transactOrThrow {
            prepareStatement(
                """
                insert into accounts (name, description, balance, type, budget_id, id)
                values (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            )
                .use { preparedStatement: PreparedStatement ->
                    val id = preparedStatement.setAccountParametersAndReturnId(
                        name,
                        description,
                        type,
                        budgetId,
                    )
                    preparedStatement.executeUpdate()
                    AccountEntity(
                        name = name,
                        description = description,
                        id = id,
                        balance = balance,
                        type = type,
                        budgetId = budgetId,
                    )
                }
                .also { insertAccountActivePeriod(it, budgetId) }
        }

    private fun Connection.insertAccountActivePeriod(account: AccountEntity, budgetId: Uuid) =
        prepareStatement(
            """
                            insert into account_active_periods (id, account_id, budget_id)
                            values (?, ?, ?)
                            on conflict do nothing
                        """.trimIndent(),
        )
            .use { createActivePeriod: PreparedStatement ->
                createActivePeriod.setUuid(1, Uuid.random())
                createActivePeriod.setUuid(2, account.id)
                createActivePeriod.setUuid(3, budgetId)
                // NOTE due to the uniqueness constraints on this table, this will be idempotent
                createActivePeriod.executeUpdate()
            }

    override fun createGeneralAccountWithId(
        id: Uuid,
        balance: BigDecimal,
        budgetId: Uuid,
    ): AccountEntity =
        dataSource.transactOrThrow {
            prepareStatement(
                """
                insert into accounts (name, description, balance, type, budget_id, id)
                values (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            )
                .use { preparedStatement: PreparedStatement ->
                    preparedStatement.setString(1, defaultGeneralAccountName)
                    preparedStatement.setString(2, defaultGeneralAccountDescription)
                    preparedStatement.setBigDecimal(3, balance)
                    preparedStatement.setString(4, AccountType.category.name)
                    preparedStatement.setUuid(5, budgetId)
                    preparedStatement.setUuid(6, id)
                    if (preparedStatement.executeUpdate() == 1) {
                        AccountEntity(
                            name = defaultGeneralAccountName,
                            description = defaultGeneralAccountDescription,
                            id = id,
                            balance = balance,
                            type = AccountType.category.name,
                            budgetId = budgetId,
                        )
                    } else
                    // NOTE probably an unneeded precaution since an SQLException would have been thrown already
                        throw IllegalArgumentException("Account already exists with accountId=$id")
                }
                .also { insertAccountActivePeriod(it, budgetId) }
        }

    private fun Connection.createRealAccountInTransaction(
        name: String,
        description: String,
        budgetId: Uuid,
        balance: BigDecimal,
    ): AccountEntity =
        prepareStatement(
            """
                    insert into accounts (name, description, balance, type, budget_id, id)
                    values (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
        )
            .use { preparedStatement: PreparedStatement ->
                val id = preparedStatement.setAccountParametersAndReturnId(
                    name,
                    description,
                    AccountType.real.name,
                    budgetId,
                    balance,
                )
                if (preparedStatement.executeUpdate() == 1) {
                    AccountEntity(
                        name = name,
                        description = description,
                        id = id,
                        type = AccountType.real.name,
                        balance = balance,
                        budgetId = budgetId,
                    )
                } else
                    throw IllegalStateException("unable to create real account for budgetId=$budgetId")
            }
            .also { insertAccountActivePeriod(it, budgetId) }

    override fun createRealAndDraftAccount(
        name: String,
        description: String,
        budgetId: Uuid,
        balance: BigDecimal,
    ): Pair<AccountEntity, AccountEntity> =
        dataSource.transactOrThrow {
            createRealAccountInTransaction(name, description, budgetId, balance)
                .let { realCompanion: AccountEntity ->
                    prepareStatement(
                        """
                            insert into accounts (name, description, balance, type, budget_id, id, companion_account_id)
                            values (?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                    )
                        .use { createDraftAccountStatement: PreparedStatement ->
                            val id = createDraftAccountStatement.setAccountParametersAndReturnId(
                                name,
                                description,
                                AccountType.draft.name,
                                budgetId,
                            )
                            createDraftAccountStatement.setUuid(7, realCompanion.id)
                            if (createDraftAccountStatement.executeUpdate() == 1) {
                                realCompanion to AccountEntity(
                                    name = name,
                                    description = description,
                                    id = id,
                                    balance = BigDecimal.ZERO.setScale(2),
                                    type = AccountType.draft.name,
                                    budgetId = budgetId,
                                    companionId = realCompanion.id,
                                )
                            } else
                            // NOTE an SQLException will probably already have been thrown
                                throw IllegalStateException("unable to create accounts for budgetId=$budgetId")
                        }
                        .also { (_, draftAccount) -> insertAccountActivePeriod(draftAccount, budgetId) }
                }
        }

    private fun PreparedStatement.setAccountParametersAndReturnId(
        name: String,
        description: String,
        type: String,
        budgetId: Uuid,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
    ): Uuid =
        Uuid.random()
            .also {
                setString(1, name)
                setString(2, description)
                setBigDecimal(3, balance)
                setString(4, type)
                setUuid(5, budgetId)
                setUuid(6, it)
            }

}
