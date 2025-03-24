package bps.budget.model

import bps.budget.persistence.AccountDao
import bps.budget.persistence.AccountDao.AccountCommitableTransactionItem
import bps.budget.persistence.AccountEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class AccountsHolder<out T : AccountData>(
    val active: List<T> = emptyList(),
    val inactive: List<T> = emptyList(),
) {

    val allAccounts: List<T> = active + inactive

    companion object {
        fun <T : AccountData> empty(): AccountsHolder<T> =
            AccountsHolder<T>()
    }
}

/**
 * Currently not thread safe to add or delete accounts.  So, just be sure to use only the "main" thread.
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

    @Suppress("UNCHECKED_CAST")
    fun <T : Account> getAccountByIdOrNull(id: Uuid): T? =
        byId[id] as T?

    fun commit(commitableTransactionItems: List<AccountCommitableTransactionItem>) {
        commitableTransactionItems
            .forEach { item: AccountCommitableTransactionItem ->
                getAccountByIdOrNull<Account>(item.accountId)!!
                    .addAmount(item.amount)
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
            val (checkingAccount: AccountEntity, draftAccount: AccountEntity) =
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
                accountDao.createAccountOrNull(
                    name = defaultWalletAccountName,
                    description = defaultWalletAccountDescription,
                    balance = walletBalance,
                    type = AccountType.real.name,
                    budgetId = budgetId,
                )!!
            val generalCategoryAccount = generalAccount.toCategoryAccount()!!
            val realWalletAccount = wallet.toRealAccount()!!
            val realCheckingAccount = checkingAccount.toRealAccount()!!
            return BudgetData(
                id = budgetId,
                name = budgetName,
                timeZone = timeZone,
                analyticsStart = Clock.System.now(),
                generalAccount = generalCategoryAccount,
                categoryAccounts =
                    AccountsHolder(
                        listOf(
                            generalCategoryAccount,
                            accountDao.createCategoryAccountOrNull(
                                name = defaultCosmeticsAccountName,
                                description = defaultCosmeticsAccountDescription,
                                budgetId = budgetId,
                            )!!.toCategoryAccount()!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultEducationAccountName,
                                defaultEducationAccountDescription,
                                budgetId = budgetId,
                            )!!.toCategoryAccount()!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultEntertainmentAccountName,
                                defaultEntertainmentAccountDescription,
                                budgetId = budgetId,
                            )!!.toCategoryAccount()!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultFoodAccountName,
                                defaultFoodAccountDescription,
                                budgetId = budgetId,
                            )!!.toCategoryAccount()!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultHobbyAccountName,
                                defaultHobbyAccountDescription,
                                budgetId = budgetId,
                            )!!.toCategoryAccount()!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultHomeAccountName,
                                defaultHomeAccountDescription,
                                budgetId = budgetId,
                            )!!.toCategoryAccount()!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultHousingAccountName,
                                defaultHousingAccountDescription,
                                budgetId = budgetId,
                            )!!.toCategoryAccount()!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultMedicalAccountName,
                                defaultMedicalAccountDescription,
                                budgetId = budgetId,
                            )!!.toCategoryAccount()!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultNecessitiesAccountName,
                                defaultNecessitiesAccountDescription,
                                budgetId = budgetId,
                            )!!.toCategoryAccount()!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultNetworkAccountName,
                                defaultNetworkAccountDescription,
                                budgetId = budgetId,
                            )!!.toCategoryAccount()!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultTransportationAccountName,
                                defaultTransportationAccountDescription,
                                budgetId = budgetId,
                            )!!.toCategoryAccount()!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultTravelAccountName,
                                defaultTravelAccountDescription,
                                budgetId = budgetId,
                            )!!.toCategoryAccount()!!,
                            accountDao.createCategoryAccountOrNull(
                                defaultWorkAccountName,
                                defaultWorkAccountDescription,
                                budgetId = budgetId,
                            )!!.toCategoryAccount()!!,
                        ),
                    ),
                realAccounts =
                    AccountsHolder(
                        listOf(
                            realWalletAccount,
                            realCheckingAccount,
                        ),
                    ),
                draftAccounts =
                    AccountsHolder(
                        listOf(
                            draftAccount.toDraftAccount {
                                if (it == realCheckingAccount.id)
                                    realCheckingAccount
                                else
                                    null
                            }!!,
                        ),
                    ),
            )
        }

    }

}
