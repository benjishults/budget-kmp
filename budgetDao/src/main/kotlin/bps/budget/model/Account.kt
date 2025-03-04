@file:OptIn(ExperimentalUuidApi::class)
package bps.budget.model

import bps.budget.model.Transaction.Type
import bps.budget.persistence.TransactionDao
import kotlinx.datetime.Instant
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// TODO consider creating all these accounts on first run
const val defaultGeneralAccountName = "General"
const val defaultGeneralAccountDescription = "Income is automatically deposited here and allowances are made from here"
const val defaultWalletAccountName = "Wallet"
const val defaultWalletAccountDescription = "Cash on hand"
const val defaultCheckingAccountName = "Checking"
const val defaultCheckingAccountDescription = "Account from which checks clear"

const val defaultCosmeticsAccountName = "Cosmetics"
const val defaultCosmeticsAccountDescription = "Cosmetics, procedures, pampering, and accessories"
const val defaultEducationAccountName = "Education"
const val defaultEducationAccountDescription = "Tuition, books, etc."
const val defaultEntertainmentAccountName = "Entertainment"
const val defaultEntertainmentAccountDescription = "Games, books, subscriptions, going out for food or fun"
const val defaultFoodAccountName = "Food"
const val defaultFoodAccountDescription = "Food other than what's covered in entertainment"
const val defaultHobbyAccountName = "Hobby"
const val defaultHobbyAccountDescription = "Expenses related to a hobby"
const val defaultHomeAccountName = "Home Upkeep"
const val defaultHomeAccountDescription = "Upkeep: association fees, furnace filters, appliances, repairs, lawn care"
const val defaultHousingAccountName = "Housing"
const val defaultHousingAccountDescription = "Rent, mortgage, property tax, insurance"
const val defaultMedicalAccountName = "Medical"
const val defaultMedicalAccountDescription = "Medicine, supplies, insurance, etc."
const val defaultNecessitiesAccountName = "Necessities"
const val defaultNecessitiesAccountDescription = "Energy, water, cleaning supplies, soap, tooth brushes, etc."
const val defaultNetworkAccountName = "Network"
const val defaultNetworkAccountDescription = "Mobile plan, routers, internet access"
const val defaultTransportationAccountName = "Transportation"
const val defaultTransportationAccountDescription = "Fares, vehicle payments, insurance, fuel, up-keep, etc."
const val defaultTravelAccountName = "Travel"
const val defaultTravelAccountDescription = "Travel expenses for vacation"
const val defaultWorkAccountName = "Work"
const val defaultWorkAccountDescription = "Work-related expenses (possibly to be reimbursed)"

/**
 * See src/test/kotlin/bps/kotlin/GenericFunctionTest.kt for a discussion of how I want to improve this
 */
abstract class Account(
    // TODO why are these vars?
    override var name: String,
    override var description: String = "",
    override val id: Uuid = Uuid.random(),
    balance: BigDecimal = BigDecimal.ZERO.setScale(2),
    open val type: String,
    val budgetId: Uuid,
) : AccountData {

    override var balance: BigDecimal = balance
        protected set

    open fun Transaction.Builder.addItemBuilderTo(
        amount: BigDecimal,
        description: String? = null,
        draftStatus: DraftStatus = DraftStatus.none,
        id: Uuid = Uuid.random(),
    ): Unit = TODO()

    open fun itemBuilderFactory(
        id: Uuid,
        amount: BigDecimal,
        description: String?,
        draftStatus: DraftStatus,
    ): Transaction.ItemBuilder<*> =
        TODO()

    open fun TransactionDao.extendedTransactionItemFactory(
        id: Uuid,
        amount: BigDecimal,
        description: String?,
        draftStatus: DraftStatus,
        transactionId: Uuid,
        transactionDescription: String,
        transactionTimestamp: Instant,
        transactionType: Type,
        accountBalanceAfterItem: BigDecimal?,
    ): TransactionDao.ExtendedTransactionItem<*> =
        TODO()

    fun commit(item: Transaction.Item<*>) {
        balance += item.amount
    }

    override fun toString(): String {
        return "${javaClass.name}('$name', $balance, id=$id, budgetId=$budgetId)"
    }

    override fun equals(other: Any?): Boolean =
        if (this === other)
            true
        else if (other !is Account)
            false
        else if (id != other.id)
            false
        else
            true

    override fun hashCode(): Int {
        return id.hashCode()
    }

}

//fun <T : Account> AccountType.daoAccountGetter(accountDao: AccountDao): (UUID, UUID) -> T? =
//    when (this) {
//        AccountType.category -> { accountId: UUID, budgetId: UUID ->
//            accountDao.getCategoryAccountOrNull(
//                accountId,
//                budgetId,
//            )
//        }
//        AccountType.real -> { accountId: UUID, budgetId: UUID ->
//            accountDao.getRealAccountOrNull(accountId, budgetId)
//        }
//        AccountType.draft -> { accountId: UUID, budgetId: UUID ->
//            accountDao.getDraftAccountOrNull(
//                accountId,
//                budgetId,
//            )
//        }
//        AccountType.charge -> { accountId: UUID, budgetId: UUID ->
//            accountDao.getRealAccountOrNull(
//                accountId,
//                budgetId,
//            )
//        }
//    }

// TODO consider merging (most of) these into a single class.

class CategoryAccount(
    name: String,
    description: String = "",
    id: Uuid = Uuid.random(),
    balance: BigDecimal = BigDecimal.ZERO.setScale(2),
    budgetId: Uuid,
) : Account(name, description, id, balance, AccountType.category.name, budgetId) {

    override fun Transaction.Builder.addItemBuilderTo(
        amount: BigDecimal,
        description: String?,
        draftStatus: DraftStatus,
        id: Uuid,
    ) {
        categoryItemBuilders.add(itemBuilderFactory(id, amount, description, draftStatus))
    }

    override fun itemBuilderFactory(
        id: Uuid,
        amount: BigDecimal,
        description: String?,
        draftStatus: DraftStatus,
    ): Transaction.ItemBuilder<CategoryAccount> =
        Transaction.ItemBuilder(
            account = this,
            id = id,
            amount = amount,
            description = description,
            draftStatus = draftStatus,
        )

    override fun TransactionDao.extendedTransactionItemFactory(
        id: Uuid,
        amount: BigDecimal,
        description: String?,
        draftStatus: DraftStatus,
        transactionId: Uuid,
        transactionDescription: String,
        transactionTimestamp: Instant,
        transactionType: Type,
        accountBalanceAfterItem: BigDecimal?,
    ): TransactionDao.ExtendedTransactionItem<CategoryAccount> =
        TransactionDao.ExtendedTransactionItem(
            item = itemBuilderFactory(
                id = id,
                amount = amount,
                description = description,
                draftStatus = draftStatus,
            ),
            transactionId = transactionId,
            transactionDescription = transactionDescription,
            transactionTimestamp = transactionTimestamp,
            transactionType = transactionType,
            transactionDao = this,
            budgetId = this@CategoryAccount.budgetId,
            accountBalanceAfterItem = accountBalanceAfterItem,
        )

    companion object : AccountFactory<CategoryAccount> {
        override fun invoke(
            name: String,
            description: String,
            id: Uuid,
            balance: BigDecimal,
            budgetId: Uuid,
            companionId: Uuid?,
        ): CategoryAccount =
            CategoryAccount(name, description, id, balance, budgetId)

        override val type: AccountType = AccountType.category

    }

}

open class RealAccount(
    name: String,
    description: String = "",
    id: Uuid = Uuid.random(),
    balance: BigDecimal = BigDecimal.ZERO.setScale(2),
    budgetId: Uuid,
) : Account(name, description, id, balance, AccountType.real.name, budgetId) {

    override fun Transaction.Builder.addItemBuilderTo(
        amount: BigDecimal,
        description: String?,
        draftStatus: DraftStatus,
        id: Uuid,
    ) {
        realItemBuilders.add(itemBuilderFactory(id, amount, description, draftStatus))
    }

    override fun TransactionDao.extendedTransactionItemFactory(
        id: Uuid,
        amount: BigDecimal,
        description: String?,
        draftStatus: DraftStatus,
        transactionId: Uuid,
        transactionDescription: String,
        transactionTimestamp: Instant,
        transactionType: Type,
        accountBalanceAfterItem: BigDecimal?,
    ): TransactionDao.ExtendedTransactionItem<RealAccount> =
        TransactionDao.ExtendedTransactionItem(
            item = itemBuilderFactory(
                id = id,
                amount = amount,
                description = description,
                draftStatus = draftStatus,
            ),
            transactionId = transactionId,
            transactionDescription = transactionDescription,
            transactionTimestamp = transactionTimestamp,
            transactionType = transactionType,
            transactionDao = this,
            budgetId = this@RealAccount.budgetId,
            accountBalanceAfterItem = accountBalanceAfterItem,
        )

    override fun itemBuilderFactory(
        id: Uuid,
        amount: BigDecimal,
        description: String?,
        draftStatus: DraftStatus,
    ): Transaction.ItemBuilder<RealAccount> =
        Transaction.ItemBuilder(
            account = this,
            id = id,
            amount = amount,
            description = description,
            draftStatus = draftStatus,
        )

    companion object : AccountFactory<RealAccount> {
        override fun invoke(
            name: String,
            description: String,
            id: Uuid,
            balance: BigDecimal,
            budgetId: Uuid,
            companionId: Uuid?,
        ): RealAccount =
            RealAccount(name, description, id, balance, budgetId)

        override val type: AccountType = AccountType.real

    }
}

/**
 * A separate [DraftAccount] is useful for quickly determining the outstanding balance.  One only has to look at the
 * balance on this account to compute the draft balance of the companion real account.
 */
class DraftAccount(
    name: String,
    description: String = "",
    id: Uuid = Uuid.random(),
    balance: BigDecimal = BigDecimal.ZERO.setScale(2),
    val realCompanion: RealAccount,
    budgetId: Uuid,
) : Account(name, description, id, balance, AccountType.draft.name, budgetId) {

    override fun Transaction.Builder.addItemBuilderTo(
        amount: BigDecimal,
        description: String?,
        draftStatus: DraftStatus,
        id: Uuid,
    ) {
        draftItemBuilders.add(itemBuilderFactory(id, amount, description, draftStatus))
    }

    override fun TransactionDao.extendedTransactionItemFactory(
        id: Uuid,
        amount: BigDecimal,
        description: String?,
        draftStatus: DraftStatus,
        transactionId: Uuid,
        transactionDescription: String,
        transactionTimestamp: Instant,
        transactionType: Type,
        accountBalanceAfterItem: BigDecimal?,
    ): TransactionDao.ExtendedTransactionItem<DraftAccount> =
        TransactionDao.ExtendedTransactionItem(
            item = itemBuilderFactory(
                id = id,
                amount = amount,
                description = description,
                draftStatus = draftStatus,
            ),
            transactionId = transactionId,
            transactionDescription = transactionDescription,
            transactionTimestamp = transactionTimestamp,
            transactionType = transactionType,
            transactionDao = this,
            budgetId = this@DraftAccount.budgetId,
            accountBalanceAfterItem = accountBalanceAfterItem,
        )

    override fun itemBuilderFactory(
        id: Uuid,
        amount: BigDecimal,
        description: String?,
        draftStatus: DraftStatus,
    ): Transaction.ItemBuilder<DraftAccount> =
        Transaction.ItemBuilder(
            account = this,
            id = id,
            amount = amount,
            description = description,
            draftStatus = draftStatus,
        )

    companion object {
        operator fun invoke(realAccountFinder: (Uuid) -> RealAccount) =
            object : AccountFactory<DraftAccount> {
                override fun invoke(
                    name: String,
                    description: String,
                    id: Uuid,
                    balance: BigDecimal,
                    budgetId: Uuid,
                    companionId: Uuid?,
                ) =
                    DraftAccount(
                        name,
                        description,
                        id,
                        balance,
                        realAccountFinder(companionId!!),
                        budgetId,
                    )

                override val type: AccountType = AccountType.draft

            }
    }

}

class ChargeAccount(
    name: String,
    description: String = "",
    id: Uuid = Uuid.random(),
    balance: BigDecimal = BigDecimal.ZERO.setScale(2),
    budgetId: Uuid,
) : RealAccount(name, description, id, balance, budgetId) {

    override val type: String = AccountType.charge.name

    override fun Transaction.Builder.addItemBuilderTo(
        amount: BigDecimal,
        description: String?,
        draftStatus: DraftStatus,
        id: Uuid,
    ) {
        chargeItemBuilders.add(itemBuilderFactory(id, amount, description, draftStatus))
    }

    override fun itemBuilderFactory(
        id: Uuid,
        amount: BigDecimal,
        description: String?,
        draftStatus: DraftStatus,
    ): Transaction.ItemBuilder<ChargeAccount> =
        Transaction.ItemBuilder(
            account = this,
            id = id,
            amount = amount,
            description = description,
            draftStatus = draftStatus,
        )

    override fun TransactionDao.extendedTransactionItemFactory(
        id: Uuid,
        amount: BigDecimal,
        description: String?,
        draftStatus: DraftStatus,
        transactionId: Uuid,
        transactionDescription: String,
        transactionTimestamp: Instant,
        transactionType: Type,
        accountBalanceAfterItem: BigDecimal?,
    ): TransactionDao.ExtendedTransactionItem<ChargeAccount> =
        TransactionDao.ExtendedTransactionItem(
            item = itemBuilderFactory(
                id = id,
                amount = amount,
                description = description,
                draftStatus = draftStatus,
            ),
            transactionId = transactionId,
            transactionDescription = transactionDescription,
            transactionTimestamp = transactionTimestamp,
            transactionType = transactionType,
            transactionDao = this,
            budgetId = this@ChargeAccount.budgetId,
            accountBalanceAfterItem = accountBalanceAfterItem,
        )

    companion object : AccountFactory<ChargeAccount> {
        override fun invoke(
            name: String,
            description: String,
            id: Uuid,
            balance: BigDecimal,
            budgetId: Uuid,
            companionId: Uuid?,
        ): ChargeAccount =
            ChargeAccount(name, description, id, balance, budgetId)

        override val type: AccountType = AccountType.charge

    }

}
