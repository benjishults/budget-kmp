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
    fun deactivateAccount(accountId: Uuid): Boolean = TODO()

    /**
     * The default implementation throws [NotImplementedError]
     */
    fun getActiveAccounts(
        type: String,
        budgetId: Uuid,
//        factory: AccountFactory<T>,
    ): List<AccountEntity> = TODO()

    fun List<AccountCommitableTransactionItem>.updateBalances(budgetId: Uuid)

    fun updateAccount(
        id: Uuid,
        name: String,
        description: String,
        budgetId: Uuid,
    ): Boolean

    fun createAccountOrNull(
        name: String,
        description: String,
        type: String,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
        budgetId: Uuid,
    ): AccountEntity?

    fun createCategoryAccountOrNull(
        name: String,
        description: String,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
        budgetId: Uuid,
    ): AccountEntity? =
        createAccountOrNull(name, description, AccountType.category.name, balance, budgetId)

    fun createRealAccountOrNull(
        name: String,
        description: String,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
        budgetId: Uuid,
    ): AccountEntity? =
        createAccountOrNull(name, description, AccountType.real.name, balance, budgetId)

    fun createChargeAccountOrNull(
        name: String,
        description: String,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
        budgetId: Uuid,
    ): AccountEntity? =
        createAccountOrNull(name, description, AccountType.charge.name, balance, budgetId)

    fun createGeneralAccountWithIdOrNull(
        id: Uuid,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
        budgetId: Uuid,
    ): AccountEntity?

    fun createRealAndDraftAccountOrNull(
        name: String,
        description: String,
        budgetId: Uuid,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
    ): Pair<AccountEntity, AccountEntity>?

}
