package bps.budget.model

import bps.budget.persistence.AccountDao
import bps.budget.persistence.TransactionDao
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class AccountsHolder<out T : Account>(
    val active: List<T> = emptyList(),
    val inactive: List<T> = emptyList(),
) {

    val allAccounts: List<T> = active + inactive

    companion object {
        fun <T : Account> empty() = AccountsHolder<T>()
    }
}

/**
 * Currently not thread safe to add or delete accounts.  So, just be sure to use only a "main" thread.
 */
@OptIn(ExperimentalUuidApi::class)
class BudgetData(
    val id: Uuid,
    val name: String,
    var timeZone: TimeZone,
    var analyticsStart: Instant,
    val generalAccount: CategoryAccount,
    categoryAccounts: AccountsHolder<CategoryAccount>,
    realAccounts: AccountsHolder<RealAccount> = AccountsHolder.empty(),
    chargeAccounts: AccountsHolder<ChargeAccount> = AccountsHolder.empty(),
    draftAccounts: AccountsHolder<DraftAccount> = AccountsHolder.empty(),
) {

    var categoryAccounts: List<CategoryAccount> = categoryAccounts.active.sortedBy { it.name }
        private set

    var realAccounts: List<RealAccount> = realAccounts.active.sortedBy { it.name }
        private set

    var draftAccounts: List<DraftAccount> = draftAccounts.active.sortedBy { it.name }
        private set

    var chargeAccounts: List<ChargeAccount> = chargeAccounts.active.sortedBy { it.name }
        private set

    init {
        require(generalAccount in categoryAccounts.active) { "general account must be among category accounts" }
    }

    private val byId: MutableMap<Uuid, Account> =
        (categoryAccounts.active + categoryAccounts.inactive +
                realAccounts.active + realAccounts.inactive +
                draftAccounts.active + draftAccounts.inactive +
                chargeAccounts.active + chargeAccounts.inactive)
            .associateByTo(mutableMapOf()) {
                it.id
            }

    val accountIdToAccountMap: Map<Uuid, Account>
        get() = byId

    @Suppress("UNCHECKED_CAST")
    fun <T : Account> getAccountByIdOrNull(id: Uuid): T? =
        byId[id] as T?

    fun commit(transaction: Transaction) {
        transaction.allItems()
            .forEach { item: Transaction.Item<*> ->
                getAccountByIdOrNull<Account>(item.account.id)!!
                    .commit(item)
            }
    }

    /**
     * Balances sum up properly and there is a general account.
     */
    fun validate(): Boolean {
        val categoryAndDraftSum: BigDecimal =
            (categoryAccounts + draftAccounts)
                .fold(BigDecimal.ZERO.setScale(2)) { sum: BigDecimal, account: Account ->
                    sum + account.balance
                }
        val realSum: BigDecimal =
            (realAccounts + chargeAccounts)
                .fold(BigDecimal.ZERO.setScale(2)) { sum: BigDecimal, account: Account ->
                    sum + account.balance
                }
        return categoryAndDraftSum.setScale(2) == realSum.setScale(2) &&
                categoryAccounts.any { it.id == generalAccount.id }
    }

    override fun toString(): String =
        buildString {
            append("BudgetData($generalAccount")
            ((categoryAccounts - generalAccount) + realAccounts + chargeAccounts + draftAccounts)
                .forEach {
                    append(", $it")
                }
            append(")")
        }

    fun addChargeAccount(chargeAccount: ChargeAccount) {
        chargeAccounts = (chargeAccounts + chargeAccount).sortedBy { it.name }
        byId[chargeAccount.id] = chargeAccount
    }

    fun addCategoryAccount(categoryAccount: CategoryAccount) {
        categoryAccounts = (categoryAccounts + categoryAccount).sortedBy { it.name }
        byId[categoryAccount.id] = categoryAccount
    }

    fun addRealAccount(realAccount: RealAccount) {
        realAccounts = (realAccounts + realAccount).sortedBy { it.name }
        byId[realAccount.id] = realAccount
    }

    fun addDraftAccount(draftAccount: DraftAccount) {
        draftAccounts = (draftAccounts + draftAccount).sortedBy { it.name }
        byId[draftAccount.id] = draftAccount
    }

    fun deactivateAccount(account: Account) {
        require(account.balance == BigDecimal.ZERO.setScale(2)) { "account balance must be zero before being deleted" }
        when (account) {
            is CategoryAccount -> {
                categoryAccounts = categoryAccounts - account
            }
            is ChargeAccount -> {
                chargeAccounts = chargeAccounts - account
            }
            is RealAccount -> {
                realAccounts = realAccounts - account
            }
            is DraftAccount -> {
                draftAccounts = draftAccounts - account
            }
        }
    }

    /**
     * NOTE: this allows you to delete a transaction that has already been cleared.  Callers should avoid making this
     *   mistake.
     *
     * This reverses the balance changes would have resulted from the application of this [transaction].  In other
     * words, this commits the [bps.budget.model.Transaction.Item.negate] of each of the [transaction]'s [bps.budget.model.Transaction.allItems].
     */
    fun undoTransaction(transaction: Transaction) {
        // if this transaction has already cleared?  error out.
        transaction
            .allItems()
            .map(Transaction.Item<*>::negate)
            .forEach { negatedTransactionItem ->
                negatedTransactionItem.account.commit(negatedTransactionItem)
            }
    }

    /**
     * NOTE: this allows you to delete a transaction that has already been cleared.  Callers should avoid making this
     *   mistake.
     *
     * This reverses the balance changes would have resulted from the application of the transaction associated
     * to this [transactionItem].  In other
     * words, this commits the [bps.budget.model.Transaction.Item.negate] of each of the [transactionItem]'s [bps.budget.model.Transaction.allItems].
     */
    fun undoTransactionForItem(transactionItem: TransactionDao.ExtendedTransactionItem<*>) {
        undoTransaction(transactionItem.transaction(id, accountIdToAccountMap))
    }

    companion object {

        @JvmStatic
        fun persistWithBasicAccounts(
            budgetName: String,
            timeZone: TimeZone = TimeZone.Companion.currentSystemDefault(),

            checkingBalance: BigDecimal = BigDecimal.ZERO.setScale(2),
            walletBalance: BigDecimal = BigDecimal.ZERO.setScale(2),
            generalAccountId: Uuid = Uuid.random(),
            budgetId: Uuid = Uuid.random(),
            accountDao: AccountDao,
        ): BudgetData {
            val (checkingAccount, draftAccount) =
                accountDao.createRealAndDraftAccountOrNull(
                    name = defaultCheckingAccountName,
                    description = defaultCheckingAccountDescription,
                    balance = checkingBalance,
                    budgetId = budgetId,
                )!!
            val generalAccount =
                accountDao.createGeneralAccountWithIdOrNull(
                    id = generalAccountId,
                    balance = checkingBalance + walletBalance,
                    budgetId = budgetId,
                )!!
            val wallet =
                accountDao.createRealAccountOrNull(
                    name = defaultWalletAccountName,
                    description = defaultWalletAccountDescription,
                    balance = walletBalance,
                    budgetId = budgetId,
                )!!
            return BudgetData(
                id = budgetId,
                name = budgetName,
                timeZone = timeZone,
                analyticsStart = Clock.System.now(),
                generalAccount = generalAccount,
                categoryAccounts =
                    AccountsHolder(
                        listOf(
                            generalAccount,
                            accountDao.createCategoryAccountOrNull(
                                defaultCosmeticsAccountName,
                                defaultCosmeticsAccountDescription,
                                budgetId = budgetId,
                            )!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultEducationAccountName,
                                defaultEducationAccountDescription,
                                budgetId = budgetId,
                            )!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultEntertainmentAccountName,
                                defaultEntertainmentAccountDescription,
                                budgetId = budgetId,
                            )!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultFoodAccountName,
                                defaultFoodAccountDescription,
                                budgetId = budgetId,
                            )!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultHobbyAccountName,
                                defaultHobbyAccountDescription,
                                budgetId = budgetId,
                            )!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultHomeAccountName,
                                defaultHomeAccountDescription,
                                budgetId = budgetId,
                            )!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultHousingAccountName,
                                defaultHousingAccountDescription,
                                budgetId = budgetId,
                            )!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultMedicalAccountName,
                                defaultMedicalAccountDescription,
                                budgetId = budgetId,
                            )!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultNecessitiesAccountName,
                                defaultNecessitiesAccountDescription,
                                budgetId = budgetId,
                            )!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultNetworkAccountName,
                                defaultNetworkAccountDescription,
                                budgetId = budgetId,
                            )!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultTransportationAccountName,
                                defaultTransportationAccountDescription,
                                budgetId = budgetId,
                            )!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultTravelAccountName,
                                defaultTravelAccountDescription,
                                budgetId = budgetId,
                            )!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultWorkAccountName,
                                defaultWorkAccountDescription,
                                budgetId = budgetId,
                            )!!,
                        ),
                    ),
                realAccounts =
                    AccountsHolder(
                        listOf(
                            wallet,
                            checkingAccount,
                        ),
                    ),
                draftAccounts =
                    AccountsHolder(listOf(draftAccount)),
            )
        }

    }

}
