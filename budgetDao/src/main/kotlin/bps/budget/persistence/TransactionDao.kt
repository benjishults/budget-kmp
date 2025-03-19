package bps.budget.persistence

import bps.budget.model.Account
import bps.budget.model.ChargeAccount
import bps.budget.model.DraftAccount
import bps.budget.model.Transaction
import bps.budget.model.TransactionItem
import bps.budget.model.TransactionType
import kotlinx.datetime.Instant
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface TransactionDao {

    fun commit(
        transaction: Transaction,
        budgetId: Uuid,
        accountDao: AccountDao,
        saveBalances: Boolean = true,
    ) {
    }

    /**
     * This implementation throws a [NotImplementedError].
     * @return the list of [BalanceToAdd]s that should be applied to correct balances on accounts.
     * @param accountIdToAccountMap must contain a mapping from **all** account IDs (including deactivated accounts).
     */
    // TODO consider options rather than this map:
    //      1. use (Uuid) -> Account rather than map
    //      2. pass in the AccountDao and just look things up rather than the map
    //      I think I like option 1 above because it leaves us open to more possibilities
    fun deleteTransaction(
        transactionId: Uuid,
        budgetId: Uuid,
        accountIdToAccountMap: Map<Uuid, Account>,
    ): List<AccountDao.BalanceToAdd> = TODO()

    fun clearCheck(
        draftTransactionItems: List<Transaction.Item<DraftAccount>>,
        clearingTransaction: Transaction,
        budgetId: Uuid,
        accountDao: AccountDao,
    ) = Unit

    fun clearCheck(
        draftTransactionItem: Transaction.Item<DraftAccount>,
        clearingTransaction: Transaction,
        budgetId: Uuid,
        accountDao: AccountDao,
    ) =
        clearCheck(listOf(draftTransactionItem), clearingTransaction, budgetId, accountDao)

    fun commitCreditCardPayment(
        clearedItems: List<ExtendedTransactionItem<ChargeAccount>>,
        billPayTransaction: Transaction,
        budgetId: Uuid,
        accountDao: AccountDao,
    ) {
    }

    /**
     * @param balanceAtEndOfPage must be provided unless [offset] is `0`.
     * If not provided, then the balance from the account will be used.
     * Its value should be the balance of the account at the point when this page of results ended.
     * @param types if non-empty, only transactions of the given types will be returned.
     */
    fun <A : Account> fetchTransactionItemsInvolvingAccount(
        account: A,
        limit: Int = 30,
        offset: Int = 0,
        types: List<TransactionType> = emptyList(),
        balanceAtEndOfPage: BigDecimal? =
            require(offset == 0) { "balanceAtEndOfPage must be provided unless offset is 0." }
                .let { account.balance },
    ): List<ExtendedTransactionItem<A>> =
        emptyList()

    /**
     *
     * @param accountIdToAccountMap must contain a mapping from **all** account IDs (including deactivated accounts).
     */
    // TODO consider options rather than this map:
    //      1. use (Uuid) -> Account rather than map
    //      2. pass in the AccountDao and just look things up rather than the map
    //      I think I like option 1 above because it leaves us open to more possibilities
    fun getTransactionOrNull(
        transactionId: Uuid,
        budgetId: Uuid,
        accountIdToAccountMap: Map<Uuid, Account>,
    ): Transaction?

    /**
     * See src/test/kotlin/bps/kotlin/GenericFunctionTest.kt for a discussion of how I want to improve this
     */
    class ExtendedTransactionItem<out A : Account>(
        val item: Transaction.ItemBuilder<A>,
        val accountBalanceAfterItem: BigDecimal?,
        val transactionId: Uuid,
        val transactionDescription: String,
        val transactionTimestamp: Instant,
        val transactionType: TransactionType,
        val transactionDao: TransactionDao,
        val budgetId: Uuid,
    ) : TransactionItem<A>,
        Comparable<ExtendedTransactionItem<*>> {

        /**
         * The first time this is referenced, a call will be made to the DB to fetch the entire transaction.
         * So, refer to this only if you need more than just the [transactionId], [transactionDescription], or
         * [transactionTimestamp].
         * @param accountIdToAccountMap must contain a mapping from **all** account IDs (including deactivated accounts).
         */
        // TODO consider options rather than this map:
        //      1. use (Uuid) -> Account rather than map
        //      2. pass in the AccountDao and just look things up rather than the map
        //      I think I like option 1 above because it leaves us open to more possibilities
        fun transaction(budgetId: Uuid, accountIdToAccountMap: Map<Uuid, Account>): Transaction =
            transaction
                ?: run {
                    transactionDao.getTransactionOrNull(transactionId, budgetId, accountIdToAccountMap)!!
                        .also {
                            transaction = it
                        }
                }

        // TODO be sure the protect this if we go multithreaded
        private var transaction: Transaction? = null

        override fun compareTo(other: ExtendedTransactionItem<*>): Int =
            this.transactionTimestamp.compareTo(other.transactionTimestamp)
                .let {
                    when (it) {
                        0 -> this.transactionId.toString().compareTo(other.transactionId.toString())
                        else -> it
                    }
                }

        override val amount: BigDecimal = item.amount
        override val description: String? = item.description
        override val account: A = item.account
        override val timestamp: Instant = transactionTimestamp

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ExtendedTransactionItem<*>) return false

            if (item != other.item) return false

            return true
        }

        override fun hashCode(): Int {
            return item.hashCode()
        }

        override fun toString(): String {
            return "ExtendedTransactionItem(transactionId=$transactionId, item=$item, accountBalanceAfterItem=$accountBalanceAfterItem)"
        }
    }
}
