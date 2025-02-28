package bps.budget.persistence

import bps.budget.model.Account
import bps.budget.model.AccountFactory
import bps.budget.model.AccountType
import bps.budget.model.CategoryAccount
import bps.budget.model.ChargeAccount
import bps.budget.model.DraftAccount
import bps.budget.model.RealAccount
import java.math.BigDecimal
import java.util.UUID

interface AccountDao {

    data class BalanceToAdd(
        val accountId: UUID,
        val amountToRevert: BigDecimal,
    )

    fun <T : Account> getAccountOrNull(
        accountId: UUID,
        budgetId: UUID,
        accountFactory: AccountFactory<T>,
    ): T? =
        TODO()

    fun getRealAccountOrNull(accountId: UUID, budgetId: UUID): RealAccount? =
        getAccountOrNull(accountId, budgetId, RealAccount)

    fun getCategoryAccountOrNull(accountId: UUID, budgetId: UUID): CategoryAccount? =
        getAccountOrNull(accountId, budgetId, CategoryAccount)

    fun getChargeAccountOrNull(accountId: UUID, budgetId: UUID): ChargeAccount? =
        getAccountOrNull(accountId, budgetId, ChargeAccount)

    fun getDraftAccountOrNull(accountId: UUID, budgetId: UUID): DraftAccount? =
        TODO()

    /**
     * The default implementation throws [NotImplementedError]
     */
    fun <T : Account> getDeactivatedAccounts(
        type: String,
        budgetId: UUID,
        factory: AccountFactory<T>,
    ): List<T> = TODO()

    /**
     * The default implementation calls [getActiveAccounts] and [getDeactivatedAccounts] and pulls the
     * [Account.name]s out.  Implementors could improve on the efficiency if desired.
     */
    fun getAllAccountNamesForBudget(budgetId: UUID): List<String> =
        buildList {
            mapOf(
                AccountType.category.name to CategoryAccount,
                AccountType.real.name to RealAccount,
                AccountType.charge.name to ChargeAccount,
            )
                .forEach { (type, factory) ->
                    addAll(
                        getActiveAccounts(type, budgetId, factory)
                            .map { it.name },
                    )
                    addAll(
                        getDeactivatedAccounts(type, budgetId, factory)
                            .map { it.name },
                    )
                }
        }

    /**
     * The default implementation throws [NotImplementedError]
     */
    fun deactivateAccount(account: Account): Unit = TODO()

    /**
     * The default implementation throws [NotImplementedError]
     */
    fun <T : Account> getActiveAccounts(
        type: String,
        budgetId: UUID,
        factory: AccountFactory<T>,
    ): List<T> = TODO()

    fun List<BalanceToAdd>.updateBalances(budgetId: UUID)
    fun updateAccount(account: Account): Boolean
    fun createCategoryAccountOrNull(
        name: String,
        description: String,
        budgetId: UUID,
    ): CategoryAccount?

    fun createGeneralAccountWithIdOrNull(
        id: UUID,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
        budgetId: UUID,
    ): CategoryAccount?

    fun createRealAccountOrNull(
        name: String,
        description: String,
        budgetId: UUID,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
    ): RealAccount?

    fun createRealAndDraftAccountOrNull(
        name: String,
        description: String,
        budgetId: UUID,
        balance: BigDecimal = BigDecimal.ZERO.setScale(2),
    ): Pair<RealAccount, DraftAccount>?

    fun createChargeAccountOrNull(
        name: String,
        description: String,
        budgetId: UUID,
    ): ChargeAccount?

}
