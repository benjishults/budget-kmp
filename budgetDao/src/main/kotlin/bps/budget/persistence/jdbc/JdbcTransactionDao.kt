package bps.budget.persistence.jdbc

import bps.budget.model.AccountData
import bps.budget.model.AccountType
import bps.budget.model.DraftStatus
import bps.budget.model.TransactionType
import bps.budget.persistence.AccountDao
import bps.budget.persistence.AccountEntity
import bps.budget.persistence.AccountTransactionEntity
import bps.budget.persistence.AllocatedItemEntities
import bps.budget.persistence.TransactionDao
import bps.budget.persistence.TransactionData
import bps.budget.persistence.TransactionEntity
import bps.budget.persistence.TransactionItemEntity
import bps.budget.persistence.allocateItemsByAccountType
import bps.jdbc.JdbcFixture
import bps.jdbc.JdbcFixture.Companion.transactOrThrow
import kotlinx.datetime.Instant
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Suppress("SqlSourceToSinkFlow")
@OptIn(ExperimentalUuidApi::class)
class JdbcTransactionDao(
//    val errorStateTracker: JdbcDao.ErrorStateTracker,
    val dataSource: DataSource,
) : TransactionDao(), JdbcFixture {

    // TODO I would like these entity classes to be constructible only by DAO classes.
    //      Options:
    //      1. make these interfaces with private implementations and have the DAO classes ensure that those passed in
    //         are of the right type
    //      2. I can't get anything else to work...
    private class JdbcTransactionEntity(
        override val id: Uuid,
        override val description: String,
        override val timestamp: Instant,
        override val transactionType: String,
        override val clearedById: Uuid? = null,
        override val items: List<TransactionItemEntity>,
        override val budgetId: Uuid,
    ) : TransactionEntity {

        override fun compareTo(other: TransactionData): Int =
            this.timestamp.compareTo(other.timestamp)
                .let {
                    when (it) {
                        0 -> this.id.toString().compareTo(other.id.toString())
                        else -> it
                    }
                }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TransactionEntity) return false

            if (id != other.id) return false
            if (budgetId != other.budgetId) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + budgetId.hashCode()
            return result
        }

        override fun toString(): String {
            return "TransactionEntity(id=$id, description='$description', timestamp=$timestamp, transactionType=$transactionType, clearedById=$clearedById, items=$items, budgetId=$budgetId)"
        }

    }

    private class JdbcTransactionItemEntity(
        override val id: Uuid,
        override val amount: BigDecimal,
        override val description: String?,
        override val accountId: Uuid,
        override val accountType: String,
        override val draftStatus: String,
        override val transactionId: Uuid,
    ) : TransactionItemEntity {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TransactionItemEntity) return false

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }

        override fun toString(): String {
            return "TransactionItemEntity(id=$id, amount=$amount, description=$description, accountId=$accountId, transactionId=$transactionId)" // , timestamp=$timestamp)"
        }

    }

    private class JdbcAccountTransactionEntity(
        override val id: Uuid,
        override val transactionId: Uuid,
        override val amount: BigDecimal,
        override val balance: BigDecimal?,
        override val transactionType: String,
        override val description: String?,
        override val transactionDescription: String?,
        override val accountId: Uuid,
        override val timestamp: Instant,
        override val draftStatus: String,
        override val budgetId: Uuid,
    ) : Comparable<AccountTransactionEntity>, AccountTransactionEntity {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AccountTransactionEntity) return false

            if (id != other.id) return false
            if (budgetId != other.budgetId) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + budgetId.hashCode()
            return result
        }

        override fun toString(): String {
            return "AccountTransactionEntity(id=$id, " +
                    "transactionId=$transactionId, " +
                    "amount=$amount, " +
                    "balanceAfterItem=$balance, " +
                    "type=$transactionType, " +
                    "description=$description, " +
                    "accountId=$accountId, " +
                    "timestamp=$timestamp, " +
                    "budgetId=$budgetId)"
        }

        override fun compareTo(other: AccountTransactionEntity): Int =
            this.timestamp.compareTo(other.timestamp)
                .let {
                    when (it) {
                        0 -> this.transactionId.toString().compareTo(other.transactionId.toString())
                        else -> it
                    }
                }

    }

    /**
     * 1. Sets the draft status to `cleared` for draft item in [clearedDraftTransactionId]
     * 2. Sets the "cleared-by" relation in the DB between the new transaction and the [clearedDraftTransactionId]
     * 3. Commits a new transaction with the given [itemsForClearingTransaction]
     * 4. Updates account balances
     * @param clearedDraftTransactionId must refer to a transaction with a single draft item and a single category item.
     * @return the new [TransactionEntity]
     * @throws IllegalArgumentException if either the [clearedDraftTransactionId] or the [itemsForClearingTransaction] is not what
     * we expect
     * @throws IllegalStateException if account ID of [clearedDraftTransactionId]'s draft item does not exist in [accountDao]
     */
    override fun createClearCheckTransaction(
        clearedDraftTransactionId: Uuid,
        description: String,
        timestamp: Instant,
        itemsForClearingTransaction: List<ClearingTransactionItem>,
        budgetId: Uuid,
        accountDao: AccountDao,
    ): TransactionEntity? =
        dataSource.transactOrThrow {
            // TODO consider exposing a transaction-less version of getAccountOrNull so that these can be done atomically
            val idToAccountMap: (Uuid) -> AccountEntity? = {
                with(accountDao) {
                    this.getAccountOrNull(it, budgetId)
                }
            }
            // require clearTransaction is a simple draft transaction(s) clearing transaction
            // with a single real item
            require(itemsForClearingTransaction.isNotEmpty())
            require(itemsForClearingTransaction.firstOrNull { it.accountType == AccountType.category.name || it.accountType == AccountType.charge.name } === null)
            val realClearingItemList: List<ClearingTransactionItem> =
                itemsForClearingTransaction.filter { it.accountType == AccountType.real.name }
            require(realClearingItemList.size == 1)
            val realTransactionItem: ClearingTransactionItem = realClearingItemList.first()
            val realAccountId: Uuid = realTransactionItem.accountId
            // the clearing transaction's draft item is on the drafts account related to that real account
            val clearedDraftTransaction: TransactionEntity =
                getTransactionOrNull(clearedDraftTransactionId, budgetId)
                    ?: throw IllegalArgumentException("clearedDraftTransactionId=$clearedDraftTransactionId not found")
            val allocatedClearedItems = clearedDraftTransaction.allocateItemsByAccountType(idToAccountMap)
            require(allocatedClearedItems.draft.size == 1)
            val clearedDraft =
                allocatedClearedItems
                    .draft
                    .first()
            // the items that we're clearing were on the same account that the new transaction is on
            // and they were outstanding
            require(
                idToAccountMap(clearedDraft.accountId)
                    ?.companionId == realAccountId,
            )
            require(
                allocatedClearedItems.real.isEmpty() &&
                        allocatedClearedItems.other.isEmpty() &&
                        allocatedClearedItems.charge.isEmpty() &&
                        allocatedClearedItems.category.isNotEmpty(),
            )
            // the cleared draft transactions items' amount sum is the amount being taken out of the real account
            require(
                allocatedClearedItems
                    .draft
                    .fold(BigDecimal.ZERO.setScale(2)) { sum, transactionItem ->
                        sum + transactionItem.amount
                    } ==
                        -realTransactionItem.amount,
            )
            prepareStatement(
                """
                            |update transaction_items ti
                            |set draft_status = 'cleared'
                            |where ti.budget_id = ?
                            |and ti.id = ?
                            |and ti.draft_status = 'outstanding'
                        """.trimMargin(),
            )
                .use { statement ->
                    statement.setUuid(1, budgetId)
                    statement.setUuid(2, clearedDraft.id)
                    if (statement.executeUpdate() != 1)
                        throw IllegalStateException("Check being cleared not found in DB")
                }
            val newTransactionId = insertTransaction(
                description = description,
                timestamp = timestamp,
                transactionType = TransactionType.clearing.name,
                budgetId = budgetId,
            )
            prepareStatement(
                """
                            |update transactions t
                            |set cleared_by_transaction_id = ?
                            |where t.id = ?
                            |and t.budget_id = ?
                        """.trimMargin(),
            )
                .use { statement ->
                    statement.setUuid(1, newTransactionId)
                    statement.setUuid(2, clearedDraftTransaction.id)
                    statement.setUuid(3, budgetId)
                    if (statement.executeUpdate() != 1)
                        throw IllegalStateException("Check being cleared not found clearedDraftTransactionId=${clearedDraftTransaction.id}")
                }
            val balancesToAdd: List<BalanceToAdd> =
                insertTransactionItems(
                    newTransactionId,
                    itemsForClearingTransaction
                        .map {
                            TransactionItem(
                                amount = it.amount,
                                description = it.description,
                                accountId = it.accountId,
                                accountType = it.accountType,
                                draftStatus = it.draftStatus,
                            )
                        },
                    budgetId,
                )
                    .also {
                        with(accountDao) {
                            this@transactOrThrow.updateBalances(it, budgetId)
                        }
                    }
            createTransactionEntity(
                newTransactionId = newTransactionId,
                description = description,
                timestamp = timestamp,
                items = itemsForClearingTransaction,
                balancesToAdd = balancesToAdd,
                budgetId = budgetId,
            )
        }

    /**
     * @return a [Pair] of the new transaction ID and the prepared statement to insert it
     */
    private fun Connection.insertTransaction(
        description: String,
        timestamp: Instant,
        transactionType: String,
        budgetId: Uuid,
    ): Uuid =
        Uuid.random()
            .also { transactionId: Uuid ->
                prepareStatement(
                    """
                insert into transactions (id, description, timestamp_utc, type, budget_id)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
                )
                    .use { insertTransaction: PreparedStatement ->
                        insertTransaction.setUuid(1, transactionId)
                        insertTransaction.setString(2, description)
                        insertTransaction.setInstant(3, timestamp)
                        insertTransaction.setString(4, transactionType)
                        insertTransaction.setUuid(5, budgetId)
                        if (insertTransaction.use { it.executeUpdate() != 1 })
                        // NOTE this precaution is probably unneeded since an SQLException would have been thrown already.
                            throw IllegalStateException("Transaction insert failed with new UUID: $transactionId")
                    }
            }

    private fun Connection.insertTransactionItems(
        transactionId: Uuid,
        items: List<TransactionItem>,
        budgetId: Uuid,
    ): List<BalanceToAdd> {
        val transactionItemCounter = items.size
        val insertSql = buildString {
            var counter = transactionItemCounter
            append("insert into transaction_items (id, transaction_id, description, amount, draft_status, budget_id, account_id) values ")
            if (counter-- > 0) {
                append("(?, ?, ?, ?, ?, ?, ?)")
                while (counter-- > 0) {
                    append(", (?, ?, ?, ?, ?, ?, ?)")
                }
            }
        }
        var parameterIndex = 1
        prepareStatement(insertSql)
            .use { transactionItemInsert: PreparedStatement ->
                val balancesToAdd: List<BalanceToAdd> = buildList {
                    items
                        .forEach { transactionItem: TransactionItem ->
                            val transactionItemId = Uuid.random()
                            parameterIndex +=
                                setStandardProperties(
                                    transactionItemId,
                                    transactionItemInsert,
                                    parameterIndex,
                                    transactionId,
                                    transactionItem,
                                    budgetId,
                                )
                            transactionItemInsert.setUuid(parameterIndex++, transactionItem.accountId)
                            add(BalanceToAdd(transactionItem.accountId, transactionItemId, transactionItem.amount))
                        }
                }
                return if (transactionItemInsert.executeUpdate() == items.size)
                    balancesToAdd
                else
                // NOTE this is probably an unneeded precaution since an SQLException would have already been thrown
                    throw IllegalStateException("Unable to insert all transaction items.")
            }
    }

    // TODO consider combining the various transaction commit functions into one
    override fun createTransaction(
        description: String,
        timestamp: Instant,
        transactionType: String,
        items: List<TransactionItem>,
        saveBalances: Boolean,
        budgetId: Uuid,
        accountDao: AccountDao,
    ): TransactionEntity =
        dataSource.transactOrThrow {
            insertTransaction(
                timestamp = timestamp,
                description = description,
                transactionType = transactionType,
                budgetId = budgetId,
            )
                .let { transactionId: Uuid ->
                    insertTransactionItems(
                        transactionId,
                        items,
                        budgetId,
                    )
                        .let { balancesToAdd: List<BalanceToAdd> ->
                            if (saveBalances)
                                with(accountDao) {
                                    this@transactOrThrow.updateBalances(balancesToAdd, budgetId)
                                }
                            createTransactionEntity(
                                newTransactionId = transactionId,
                                description = description,
                                timestamp = timestamp,
                                items = items,
                                balancesToAdd = balancesToAdd,
                                budgetId = budgetId,
                            )
                        }
                }
        }

    override fun deleteTransaction(transactionId: Uuid, budgetId: Uuid, accountDao: AccountDao): List<BalanceToAdd> =
        dataSource.transactOrThrow {
            requireNotCleared(transactionId, budgetId)
            buildList {
                prepareStatement(
                    """
                    |delete from transaction_items where transaction_id = ? and budget_id = ?
                    |returning account_id, id, amount
                """.trimMargin(),
                )
                    .use { statement ->
                        statement.setUuid(1, transactionId)
                        statement.setUuid(2, budgetId)
                        statement.executeQuery()
                            .use { resultSet ->
                                while (resultSet.next()) {
                                    add(
                                        BalanceToAdd(
                                            resultSet.getUuid("account_id")!!,
                                            resultSet.getUuid("id")!!,
                                            -resultSet.getCurrencyAmount("amount"),
                                        ),
                                    )
                                }
                            }
                    }
            }
                .also {
                    prepareStatement("""delete from transactions where id = ? and budget_id = ?""")
                        .use { statement: PreparedStatement ->
                            statement.setUuid(1, transactionId)
                            statement.setUuid(2, budgetId)
                            if (statement.executeUpdate() != 1)
                                throw IllegalStateException("No transaction found with id $transactionId")
                        }
                    with(accountDao) { this@transactOrThrow.updateBalances(it, budgetId) }
                }
        }

    private fun Connection.requireNotCleared(transactionId: Uuid, budgetId: Uuid) {
        prepareStatement("""select * from transactions where id = ? and budget_id = ?""")
            .use { statement ->
                statement.setUuid(1, transactionId)
                statement.setUuid(2, budgetId)
                statement.executeQuery()
                    .use { resultSet ->
                        if (resultSet.next()) {
                            if (resultSet.getString("cleared_by_transaction_id") !== null) {
                                throw IllegalStateException("This transaction has already been cleared")
                            }
                        } else {
                            throw IllegalArgumentException("No transaction found with id $transactionId")
                        }
                    }
            }
    }

    /**
     * 1. Sets the [TransactionItem.draftStatus] to [DraftStatus.cleared] for each [chargeTransactionsBeingCleared]
     * 2. Sets the "cleared-by" relation in the DB.
     * 3. Commits the new [items]
     * 4. Updates account balances
     * @throws IllegalArgumentException if either the [chargeTransactionsBeingCleared] or the [items] is not what
     * we expect
     */
// TODO allow checks to pay credit card bills
// TODO abstract code between this and the check clearing one.
    override fun createCreditCardPaymentTransaction(
        chargeTransactionsBeingCleared: List<Uuid>,
        description: String,
        timestamp: Instant,
        items: List<TransactionItem>,
        budgetId: Uuid,
        // NOTE needed for:
        //      1. getting the account type of each item's account
        //      2. getting the account type of each chargeTransactionsBeingCleared's account
        //      3. updating balances
        accountDao: AccountDao,
    ): TransactionEntity? =
        dataSource.transactOrThrow {
            // TODO     If we added accountType to the TransactionItemEntity, we wouldn't need this map
            val idToAccountMap: (Uuid) -> AccountEntity? = { accountDao.getAccountOrNull(it, budgetId) }
            val allocatedClearingItems = items.allocateItemsByAccountType(idToAccountMap)
            // require billPayTransaction is a simple real transfer between a real and a charge account
            require(allocatedClearingItems.draft.isEmpty())
            require(allocatedClearingItems.category.isEmpty())
            require(allocatedClearingItems.charge.size == 1)
            require(allocatedClearingItems.real.size == 1)
            val billPayChargeTransactionItem: TransactionItem =
                allocatedClearingItems.charge.first()
            val chargeAccountId: Uuid =
                billPayChargeTransactionItem.accountId
            // require clearedItems to be what we expect
            require(chargeTransactionsBeingCleared.isNotEmpty())
            // all cleared items must be on the same charge account that's getting the transfer
            val chargeTransactionEntitiesBeingCleared: List<TransactionEntity> =
                chargeTransactionsBeingCleared
                    .map {
                        getTransactionOrNull(it, budgetId)
                            ?: throw IllegalArgumentException("No transaction found with id $it")
                    }
            val allocatedChargeTransactionsBeingCleared: List<AllocatedItemEntities> =
                chargeTransactionEntitiesBeingCleared
                    .map { it.allocateItemsByAccountType(idToAccountMap) }
            require(
                allocatedChargeTransactionsBeingCleared.all {
                    it.charge.size == 1 &&
                            it.charge.first().accountId == chargeAccountId
                },
            )
            // the amount of the clearedItems must be the same as the amount being transferred
            require(
                allocatedChargeTransactionsBeingCleared
                    .fold(BigDecimal.ZERO.setScale(2)) { sum: BigDecimal, allocated: AllocatedItemEntities ->
                        sum + allocated.charge.first().amount
                    } ==
                        -billPayChargeTransactionItem.amount,
            )
            val newTransactionId: Uuid =
                insertTransaction(
                    description = description,
                    timestamp = timestamp,
                    transactionType = TransactionType.clearing.name,
                    budgetId = budgetId,
                )
            allocatedChargeTransactionsBeingCleared
                .forEachIndexed { index: Int, allocatedChargeTransactionBeingCleared: AllocatedItemEntities ->
                    prepareStatement(
                        """
                            |update transaction_items ti
                            |set draft_status = 'cleared'
                            |where ti.budget_id = ?
                            |and ti.id = ?
                            |and ti.draft_status = 'outstanding'
                        """.trimMargin(),
                    )
                        .use { statement ->
                            statement.setUuid(1, budgetId)
                            val transactionItemId: Uuid = allocatedChargeTransactionBeingCleared.charge.first().id
                            statement.setUuid(2, transactionItemId)
                            if (statement.executeUpdate() != 1)
                            // NOTE probably unneeded precaution since an exception would already be thrown.
                                throw IllegalStateException("Charge being cleared not found in DB: transactionItemId=$transactionItemId")
                        }
                    prepareStatement(
                        """
                            |update transactions t
                            |set cleared_by_transaction_id = ?
                            |where t.id = ?
                            |and t.budget_id = ?
                        """.trimMargin(),
                    )
                        .use { statement ->
                            statement.setUuid(1, newTransactionId)
                            val transactionId: Uuid = chargeTransactionsBeingCleared[index]
                            statement.setUuid(2, transactionId)
                            statement.setUuid(3, budgetId)
                            if (statement.executeUpdate() != 1)
                            // NOTE probably unneeded precaution since an exception would already be thrown.
                                throw IllegalStateException("Charge being cleared not found in DB transactionId=$transactionId")
                        }
                }
            with(accountDao) {
                val balancesToAdd: List<BalanceToAdd> =
                    insertTransactionItems(
                        transactionId = newTransactionId,
                        items = items,
                        budgetId = budgetId,
                    )
                this@transactOrThrow.updateBalances(balancesToAdd, budgetId)
                createTransactionEntity(newTransactionId, description, timestamp, items, balancesToAdd, budgetId)
            }
        }

    fun TransactionItemData.toTransactionItemEntity(
        newTransactionId: Uuid,
        transactionItemId: Uuid,
    ): TransactionItemEntity =
        JdbcTransactionItemEntity(
            id = transactionItemId,
            amount = amount,
            description = description,
            accountId = accountId,
            accountType = accountType,
            draftStatus = draftStatus,
            transactionId = newTransactionId,
        )


    private fun createTransactionEntity(
        newTransactionId: Uuid,
        description: String,
        timestamp: Instant,
        items: List<TransactionItemData>,
        balancesToAdd: List<BalanceToAdd>,
        budgetId: Uuid,
    ): JdbcTransactionEntity = JdbcTransactionEntity(
        id = newTransactionId,
        description = description,
        timestamp = timestamp,
        transactionType = TransactionType.clearing.name,
        clearedById = null,
        items = items.mapIndexed { index, item ->
            item.toTransactionItemEntity(newTransactionId, balancesToAdd[index].transactionItemId)
        },
        budgetId = budgetId,
    )

    private fun List<TransactionItem>.allocateItemsByAccountType(idToAccount: (Uuid) -> AccountData?): AllocatedItem {
        val real = mutableListOf<TransactionItem>()
        val charge = mutableListOf<TransactionItem>()
        val draft = mutableListOf<TransactionItem>()
        val other = mutableListOf<TransactionItem>()
        val category = mutableListOf<TransactionItem>()
        forEach { item: TransactionItem ->
            when (idToAccount(item.accountId)!!.type) {
                AccountType.category.name -> category.add(item)
                AccountType.real.name -> real.add(item)
                AccountType.draft.name -> draft.add(item)
                AccountType.charge.name -> charge.add(item)
                else -> other.add(item)
            }
        }
        return object : AllocatedItem {
            override val real: List<TransactionItem> = real
            override val charge: List<TransactionItem> = charge
            override val draft: List<TransactionItem> = draft
            override val category: List<TransactionItem> = category
            override val other: List<TransactionItem> = other
        }
    }

    /**
     * @param balanceAtStartOfPage is the balance of the account after the latest transaction on the page being requested.
     * If `null`, then [AccountTransactionEntity.balance] will be `null` for each [AccountTransactionEntity] returned.
     */
    override fun fetchTransactionItemsInvolvingAccount(
        accountId: Uuid,
        limit: Int,
        offset: Int,
        types: List<String>,
        balanceAtStartOfPage: BigDecimal?,
        budgetId: Uuid,
    ): List<AccountTransactionEntity> =
        buildList {
            val actualTypes: List<String> = types.takeIf { it.isNotEmpty() } ?: TransactionType.entries.map { it.name }
            dataSource.transactOrThrow {
                // TODO if it is a clearing transaction, then create the full transaction by combining the cleared category items
                //      with this.
                prepareStatement(
                    buildString {
                        append(
                            """
                    |select t.id as transaction_id,
                    |       t.description as transaction_description,
                    |       t.timestamp_utc as transaction_timestamp,
                    |       t.type,
                    |       i.id as item_id,
                    |       i.amount,
                    |       i.description,
                    |       i.draft_status
                    |from transactions t
                    |         join transaction_items i
                    |            on i.transaction_id = t.id
                    |                and t.budget_id = i.budget_id
                    |where t.budget_id = ?
                    |  and i.account_id = ?
                    |  and t.type in (?
            """.trimMargin(),
                        )
                        actualTypes
                            .drop(1)
                            .forEach {
                                append(", ?")
                            }
                        append(")\n")
                        append(
                            """
                    |order by t.timestamp_utc desc, t.id
                    |limit ?
                    |offset ?
            """.trimMargin(),
                        )
                    },
                )
                    .use { selectExtendedTransactionItemsForAccount: PreparedStatement ->
                        selectExtendedTransactionItemsForAccount
                            .apply {
                                setUuid(1, budgetId)
                                setUuid(2, accountId)
                                actualTypes.forEachIndexed { index, type ->
                                    setString(index + 3, type)
                                }
                                setInt(actualTypes.size + 3, limit)
                                setInt(actualTypes.size + 4, offset)
                            }
                            .executeQuery()
                            .use { result: ResultSet ->
                                with(result) {
                                    var runningBalance: BigDecimal? = balanceAtStartOfPage
                                    while (next()) {
                                        val transactionId: Uuid =
                                            getUuid("transaction_id")!!
                                        val transactionTimestamp: Instant =
                                            getInstant("transaction_timestamp")
                                        val id: Uuid =
                                            getUuid("item_id")!!
                                        val amount: BigDecimal =
                                            getCurrencyAmount("amount")
                                        val description: String? =
                                            getString("description")
                                        val transactionDescription: String =
                                            getString("transaction_description")!!
                                        val draftStatus: DraftStatus =
                                            DraftStatus.valueOf(getString("draft_status"))
                                        this@buildList.add(
                                            JdbcAccountTransactionEntity(
                                                id = id,
                                                amount = amount,
                                                description = description,
                                                draftStatus = draftStatus.name,
                                                transactionId = transactionId,
                                                transactionDescription = transactionDescription,
                                                transactionType = TransactionType.valueOf(getString("type")!!).name,
                                                balance = runningBalance,
                                                accountId = accountId,
                                                timestamp = transactionTimestamp,
                                                budgetId = budgetId,
                                            ),
                                        )
                                        if (runningBalance !== null)
                                            runningBalance -= amount
                                    }
                                    // TODO https://github.com/benjishults/budget/issues/14
//                                    if (draftStatus == DraftStatus.clearing) {
//                                        prepareStatement(
//                                            """
//                                            |select t.*,
//                                            |       i.amount      as item_amount,
//                                            |       i.description as item_description,
//                                            |       i.category_account_id
//                                            |from transactions t
//                                            |         join transaction_items i on i.transaction_id = t.id
//                                            |where t.id in (select id
//                                            |               from transactions
//                                            |               where cleared_by_transaction_id = ?)
//                                            |  and i.category_account_id is not null
//                                        """.trimMargin(),
//                                        )
//                                            .use { selectClearedByExtendedTransactionItems: PreparedStatement ->
//                                                selectClearedByExtendedTransactionItems.setUuid(1, runningTransactionId!!)
//                                                selectClearedByExtendedTransactionItems.executeQuery()
//                                                    .use { result: ResultSet ->
//                                                        while (result.next()) {
//                                                            val itemAmount = result.getCurrencyAmount("item_amount")
//                                                            val itemDescription: String? =
//                                                                result.getString("item_description")
//                                                            transactionBuilder!!
//                                                                .categoryItemBuilders
//                                                                .maybeAddItemBuilder(
//                                                                    result,
//                                                                    itemAmount,
//                                                                    itemDescription,
//                                                                    data,
//                                                                    "category",
//                                                                    DraftStatus.none,
//                                                                ) { account: CategoryAccount ->
//                                                                    this.categoryAccount = account
//                                                                }
//                                                        }
//                                                    }
//                                            }
//                                    } else {
                                }
                            }
                    }
            }
        }

    override fun getTransactionOrNull(
        transactionId: Uuid,
        budgetId: Uuid,
    ): TransactionEntity? =
        dataSource.transactOrThrow {
            getTransactionOrNull(transactionId, budgetId)
        }

    override fun Connection.getTransactionOrNull(
        transactionId: Uuid,
        budgetId: Uuid,
    ): TransactionEntity? =
        prepareStatement(
            """
                    |select t.description as transaction_description,
                    |       t.timestamp_utc as transaction_timestamp,
                    |       t.cleared_by_transaction_id,
                    |       t.type,
                    |       a.type as account_type,
                    |       i.account_id,
                    |       i.id,
                    |       i.amount,
                    |       i.description,
                    |       i.draft_status
                    |from transactions t
                    |         join transaction_items i
                    |              on i.transaction_id = t.id
                    |                  and t.budget_id = i.budget_id
                    |         join accounts a
                    |               on i.account_id = a.id
                    |                  and t.budget_id = i.budget_id
                    |where t.budget_id = ?
                    |  and t.id = ?
                """.trimMargin(),
        )
            .use { selectTransactionAndItems: PreparedStatement ->
                var description: String? = null
                var timestamp: Instant? = null
                var transactionType: String? = null
                var clearedById: Uuid? = null
                selectTransactionAndItems
                    .apply {
                        setUuid(1, budgetId)
                        setUuid(2, transactionId)
                    }
                    .executeQuery()
                    .use { result: ResultSet ->
                        val itemList = buildList {
                            if (result.next()) {
                                description = result.getString("transaction_description")
                                timestamp = result.getInstant("transaction_timestamp")
                                transactionType = TransactionType.valueOf(result.getString("type")).name
                                clearedById = result.getUuid("cleared_by_transaction_id")
                                add(result.toTransactionItemEntity(transactionId))
                                while (result.next()) {
                                    add(result.toTransactionItemEntity(transactionId))
                                }
                            }
                        }
                        return itemList
                            .takeIf { it.isNotEmpty() }
                            ?.let { items: List<JdbcTransactionItemEntity> ->
                                JdbcTransactionEntity(
                                    id = transactionId,
                                    description = description!!,
                                    timestamp = timestamp!!,
                                    transactionType = transactionType!!,
                                    clearedById = clearedById,
                                    items = items,
                                    budgetId = budgetId,
                                )
                            }
                    }
            }

    private fun ResultSet.toTransactionItemEntity(
        transactionId: Uuid,
    ): JdbcTransactionItemEntity = JdbcTransactionItemEntity(
        id = getUuid("id")!!,
        amount = getCurrencyAmount("amount"),
        description = getString("description"),
        accountId = getUuid("account_id")!!,
        accountType = getString("account_type"),
        draftStatus = getString("draft_status"),
        transactionId = transactionId,
    )

    /**
     * Sets a set of standard parameter starting at [parameterIndex]
     * @return the number of parameters set.
     */
    private fun setStandardProperties(
        transactionItemId: Uuid,
        transactionItemInsert: PreparedStatement,
        parameterIndex: Int,
        transactionId: Uuid,
        transactionItem: TransactionItem,
        budgetId: Uuid,
    ): Int {
        transactionItemInsert.setUuid(parameterIndex, transactionItemId)
        transactionItemInsert.setUuid(parameterIndex + 1, transactionId)
        transactionItemInsert.setString(parameterIndex + 2, transactionItem.description)
        transactionItemInsert.setBigDecimal(parameterIndex + 3, transactionItem.amount)
        transactionItemInsert.setString(parameterIndex + 4, transactionItem.draftStatus)
        transactionItemInsert.setUuid(parameterIndex + 5, budgetId)
        return 6
    }

}
