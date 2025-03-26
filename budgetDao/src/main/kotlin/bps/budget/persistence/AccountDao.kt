package bps.budget.persistence

import bps.budget.model.AccountType
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface AccountDao {

    interface AccountCommitableTransactionItem /*: Comparable<TransactionItem<*>>*/ {
        val amount: BigDecimal
        val accountId: Uuid
    }

    fun getAccountOrNull(
        accountId: Uuid,
        budgetId: Uuid,
//        transactional: Boolean = true,
//        accountFactory: AccountFactory<T>,
    ): AccountEntity? =
        TODO()

    /**
     * The default implementation throws [NotImplementedError]
     */
    fun getDeactivatedAccounts(
        type: String,
        budgetId: Uuid,
//        factory: AccountFactory<T>,
    ): List<AccountEntity> = TODO()

    /**
     * The default implementation calls [getActiveAccounts] and [getDeactivatedAccounts] and pulls the
     * [AccountEntity.name]s out.  Implementors could improve on the efficiency if desired.
     */
    fun getAllAccountNamesForBudget(budgetId: Uuid): List<String> =
        TODO()

    /**
     * The default implementation throws [NotImplementedError]
     */
    fun deactivateAccount(accountId: Uuid): Unit = TODO()

    /**
     * The default implementation throws [NotImplementedError]
     */
    fun getActiveAccounts(
        type: String,
        budgetId: Uuid,
//        factory: AccountFactory<T>,
    ): List<AccountEntity> = TODO()

    fun List<AccountCommitableTransactionItem>.updateBalances(
        budgetId: Uuid,
//        transactional: Boolean = true,
    )

    fun updateAccount(
        id: Uuid,
        name: String,
        description: String,
        budgetId: Uuid,
    ): Boolean

    fun createAccount(
        name: String,
        description: String,
        type: String,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
        budgetId: Uuid,
    ): AccountEntity

    fun createCategoryAccountOrNull(
        name: String,
        description: String,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
        budgetId: Uuid,
    ): AccountEntity =
        createAccount(name, description, AccountType.category.name, balance, budgetId)

    fun createRealAccount(
        name: String,
        description: String,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
        budgetId: Uuid,
    ): AccountEntity =
        createAccount(name, description, AccountType.real.name, balance, budgetId)

    fun createChargeAccount(
        name: String,
        description: String,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
        budgetId: Uuid,
    ): AccountEntity =
        createAccount(name, description, AccountType.charge.name, balance, budgetId)

    fun createGeneralAccountWithId(
        id: Uuid,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
        budgetId: Uuid,
    ): AccountEntity

    fun createRealAndDraftAccount(
        name: String,
        description: String,
        budgetId: Uuid,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
    ): Pair<AccountEntity, AccountEntity>

}
