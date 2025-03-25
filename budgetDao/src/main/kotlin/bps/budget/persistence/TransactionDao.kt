package bps.budget.persistence

import bps.budget.model.DraftStatus
import bps.budget.persistence.AccountDao.AccountCommitableTransactionItem
import kotlinx.datetime.Instant
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
abstract class TransactionDao {

    data class BalanceToAdd(
        override val accountId: Uuid,
        val transactionItemId: Uuid,
        override val amount: BigDecimal,
    ) : AccountCommitableTransactionItem

    /**
     * Used when doing creates
     */
    data class TransactionItem(
        override val amount: BigDecimal,
        override val description: String? = null,
        override val accountId: Uuid,
        override val accountType: String,
        override val draftStatus: String = DraftStatus.none.name,
    ) : AccountCommitableTransactionItem, TransactionItemData

    interface TransactionItemData {
        val amount: BigDecimal
        val description: String?
        val accountId: Uuid
        val accountType: String
        val draftStatus: String

    }

    interface AllocatedItem {
        val real: List<TransactionItem>
        val charge: List<TransactionItem>
        val draft: List<TransactionItem>
        val category: List<TransactionItem>
        val other: List<TransactionItem>
    }

    /**
     * Used when creating a clearing transaction
     */
    data class ClearingTransactionItem(
        override val amount: BigDecimal,
        override val description: String? = null,
        override val accountId: Uuid,
        override val accountType: String,
//        val accountsCompanionId: Uuid?,
        override val draftStatus: String,
    ) : AccountCommitableTransactionItem, TransactionItemData

    // TODO consider combining the various transaction commit functions into one
    abstract fun createTransactionOrNull(
        description: String,
        timestamp: Instant,
        transactionType: String,
        items: List<TransactionItem>,
//        clearsId: Uuid? = null,
//        accountDao: AccountDao,
        saveBalances: Boolean = true,
        budgetId: Uuid,
        // NOTE when I'm using this accountDao, I don't get the same transactional guarantees that you might wish for.
        accountDao: AccountDao,
    ): TransactionEntity?

//    fun commit(
//        transaction: Transaction,
//        budgetId: Uuid,
//        accountDao: AccountDao,
//        saveBalances: Boolean = true,
//    ) {
//    }

    /**
     * Deletes the given transaction and all its items from the DB.
     *
     * Recommended usage--if you want balances updated, use the [AccountDao] as follows:
     *
     * ```kotlin
     *         with(accountDao) {
     *             transactionDao.deleteTransaction(
     *                 transactionId = transactionId,
     *                 budgetId = budgeId,
     *             )
     *                 .updateBalances(budgetId = budgetId)
     *        }
     * ```
     * @throws IllegalStateException if the given transaction has already been cleared.
     * @throws IllegalArgumentException if the transaction doesn't exist.
     * @return the list of [AccountDao.BalanceToAdd]s that should be applied to correct balances on accounts.
     */
    // TODO consider options rather than this map:
    //      1. use (Uuid) -> Account rather than map
    //      2. pass in the AccountDao and just look things up rather than the map
    //      I think I like option 1 above because it leaves us open to more possibilities
    abstract fun deleteTransaction(
        transactionId: Uuid,
        budgetId: Uuid,
//     * @param accountIdToAccountMap must contain a mapping from **all** account IDs (including deactivated accounts).
//        accountIdToAccountMap: Map<Uuid, Account>,
    ): List<BalanceToAdd>

//    fun clearCheck(
//        draftTransactionItems: List<Transaction.Item<DraftAccount>>,
//        clearingTransaction: Transaction,
//        budgetId: Uuid,
//        accountDao: AccountDao,
//    ) = Unit

    open fun createClearCheckTransaction(
        clearedDraftTransactionId: Uuid,
        description: String,
        timestamp: Instant,
        itemsForClearingTransaction: List<ClearingTransactionItem>,
        budgetId: Uuid,
//        idToAccountMap: (Uuid) -> AccountEntity?,
        // NOTE when I'm using this accountDao, I don't get the same transactional guarantees that you might wish for.
        accountDao: AccountDao,
    ): TransactionEntity? =
        TODO()

    open fun createCreditCardPaymentTransaction(
        chargeTransactionsBeingCleared: List<Uuid>,
        description: String,
        timestamp: Instant,
        items: List<TransactionItem>,
        budgetId: Uuid,
        // NOTE when I'm using this accountDao, I don't get the same transactional guarantees that you might wish for.
        accountDao: AccountDao,
    ): TransactionEntity? =
        TODO()

//    fun commitCreditCardPayment(
//        clearedItems: List<ExtendedTransactionItem<ChargeAccount>>,
//        billPayTransaction: Transaction,
//        budgetId: Uuid,
//        accountDao: AccountDao,
//    ) {
//    }

    /**
     * @param balanceAtStartOfPage must be provided unless [offset] is `0`.
     * If not provided, then the balance from the account will be used.
     * Its value should be the balance of the account at the point when this page of results ended.
     * @param types if non-empty, only transactions of the given types will be returned.
     */
    open fun fetchTransactionItemsInvolvingAccount(
        accountId: Uuid,
        limit: Int,
        offset: Int,
        types: List<String>,
        balanceAtStartOfPage: BigDecimal?,
        budgetId: Uuid,
    ): List<AccountTransactionEntity> =
        TODO()

//    /**
//     * @param balanceAtEndOfPage must be provided unless [offset] is `0`.
//     * If not provided, then the balance from the account will be used.
//     * Its value should be the balance of the account at the point when this page of results ended.
//     * @param types if non-empty, only transactions of the given types will be returned.
//     */
//    fun <A : Account> fetchTransactionItemsInvolvingAccount(
//        account: A,
//        limit: Int = 30,
//        offset: Int = 0,
//        types: List<TransactionType> = emptyList(),
//        balanceAtEndOfPage: BigDecimal? =
//            require(offset == 0) { "balanceAtEndOfPage must be provided unless offset is 0." }
//                .let { account.balance },
//    ): List<ExtendedTransactionItem<A>> =
//        emptyList()

    /**
     *
     * @param accountIdToAccountMap must contain a mapping from **all** account IDs (including deactivated accounts).
     */
    abstract fun getTransactionOrNull(
        transactionId: Uuid,
        budgetId: Uuid,
    ): TransactionEntity?

    // FIXME get rid of this class
//    /**
//     * See src/test/kotlin/bps/kotlin/GenericFunctionTest.kt for a discussion of how I want to improve this
//     */
//    class ExtendedTransactionItem<out A : Account>(
//        val item: Transaction.ItemBuilder<A>,
//        val accountBalanceAfterItem: BigDecimal?,
//        val transactionId: Uuid,
//        val transactionDescription: String,
//        val transactionTimestamp: Instant,
//        val transactionType: TransactionType,
//        val transactionDao: TransactionDao,
//        val budgetId: Uuid,
//    ) : bps.budget.model.TransactionItem<A>,
//        Comparable<ExtendedTransactionItem<*>> {
//
//        /**
//         * The first time this is referenced, a call will be made to the DB to fetch the entire transaction.
//         * So, refer to this only if you need more than just the [transactionId], [transactionDescription], or
//         * [transactionTimestamp].
//         * @param accountIdToAccountMap must contain a mapping from **all** account IDs (including deactivated accounts).
//         */
//        // TODO consider options rather than this map:
//        //      1. use (Uuid) -> Account rather than map
//        //      2. pass in the AccountDao and just look things up rather than the map
//        //      I think I like option 1 above because it leaves us open to more possibilities
//        fun transaction(budgetId: Uuid, accountIdToAccountMap: Map<Uuid, Account>): Transaction =
//            transaction
//                ?: run {
//                    transactionDao.getTransactionOrNull(transactionId, budgetId, accountIdToAccountMap)!!
//                        .also {
//                            transaction = it
//                        }
//                }
//
//        // TODO be sure the protect this if we go multithreaded
//        private var transaction: Transaction? = null
//
//        override fun compareTo(other: ExtendedTransactionItem<*>): Int =
//            this.transactionTimestamp.compareTo(other.transactionTimestamp)
//                .let {
//                    when (it) {
//                        0 -> this.transactionId.toString().compareTo(other.transactionId.toString())
//                        else -> it
//                    }
//                }
//
//        override val amount: BigDecimal = item.amount
//        override val description: String? = item.description
//        override val account: A = item.account
//        override val timestamp: Instant = transactionTimestamp
//
//        override fun equals(other: Any?): Boolean {
//            if (this === other) return true
//            if (other !is ExtendedTransactionItem<*>) return false
//
//            if (item != other.item) return false
//
//            return true
//        }
//
//        override fun hashCode(): Int {
//            return item.hashCode()
//        }
//
//        override fun toString(): String {
//            return "ExtendedTransactionItem(transactionId=$transactionId, item=$item, accountBalanceAfterItem=$accountBalanceAfterItem)"
//        }
//
//    }
}
