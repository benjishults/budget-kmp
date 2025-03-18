@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.consistency

import bps.budget.model.BudgetData
import bps.budget.model.ChargeAccount
import bps.budget.model.DraftAccount
import bps.budget.model.DraftStatus
import bps.budget.model.Transaction
import bps.budget.model.TransactionType
import bps.budget.persistence.AccountDao
import bps.budget.persistence.TransactionDao
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi

fun commitTransactionConsistently(
    transaction: Transaction,
    transactionDao: TransactionDao,
    accountDao: AccountDao,
    budgetData: BudgetData,
) {
    budgetData.commit(transaction)
    transactionDao.commit(transaction, budgetData.id, accountDao)
}

fun commitCreditCardPaymentConsistently(
    transaction: Transaction,
    allSelectedItems: List<TransactionDao.ExtendedTransactionItem<ChargeAccount>>,
    transactionDao: TransactionDao,
    accountDao: AccountDao,
    budgetData: BudgetData,
) {
    budgetData.commit(transaction)
    transactionDao.commitCreditCardPayment(
        allSelectedItems,
        transaction,
        budgetData.id,
        accountDao,
    )
}

fun clearCheckConsistently(
    draftTransactionItem: TransactionDao.ExtendedTransactionItem<DraftAccount>,
    timestamp: Instant,
    draftAccount: DraftAccount,
    transactionDao: TransactionDao,
    accountDao: AccountDao,
    budgetData: BudgetData,
): Transaction =
    Transaction.Builder(
        draftTransactionItem.transactionDescription,
        timestamp,
        transactionType = TransactionType.clearing,
    )
        .apply {
            with(draftAccount) {
                addItemBuilderTo(
                    -draftTransactionItem.amount,
                    this@apply.description,
                    DraftStatus.clearing,
                )
            }
            with(draftTransactionItem.account.realCompanion) {
                addItemBuilderTo(-draftTransactionItem.amount, this@apply.description)
            }
        }
        .build()
        .also { clearingTransaction: Transaction ->
            budgetData.commit(clearingTransaction)
            transactionDao.clearCheck(
                draftTransactionItem
                    .item
                    .build(
                        draftTransactionItem.transaction(
                            budgetId = budgetData.id,
                            accountIdToAccountMap = budgetData.accountIdToAccountMap,
                        ),
                    ),
                clearingTransaction,
                budgetData.id,
                accountDao,
            )
        }
