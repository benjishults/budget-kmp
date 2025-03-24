@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.consistency

import bps.budget.income.createInitialBalanceTransaction
import bps.budget.model.BudgetData
import bps.budget.model.CategoryAccount
import bps.budget.model.ChargeAccount
import bps.budget.model.RealAccount
import bps.budget.model.Transaction
import bps.budget.model.toCategoryAccount
import bps.budget.model.toChargeAccount
import bps.budget.model.toDraftAccount
import bps.budget.model.toRealAccount
import bps.budget.persistence.AccountDao
import bps.budget.persistence.TransactionDao
import bps.console.app.TryAgainAtMostRecentMenuException
import bps.console.inputs.SimplePromptWithDefault
import bps.console.inputs.getTimestampFromUser
import bps.console.io.WithIo
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi

fun createCategoryAccountConsistently(
    name: String,
    description: String,
    accountDao: AccountDao,
    budgetData: BudgetData,
): CategoryAccount? =
    accountDao
        .createCategoryAccountOrNull(name, description, budgetId = budgetData.id)
        ?.toCategoryAccount()
        ?.also { categoryAccount: CategoryAccount ->
            budgetData.addCategoryAccount(categoryAccount)
        }

// TODO move the stuff that needs IO back to the call site
fun WithIo.createRealAccountConsistentlyWithIo(
    name: String,
    description: String,
    balance: BigDecimal,
    isDraft: Boolean,
    transactionDao: TransactionDao,
    accountDao: AccountDao,
    budgetData: BudgetData,
    clock: Clock,
): RealAccount? =
    if (isDraft) {
        accountDao
            .createRealAndDraftAccountOrNull(
                name,
                description,
                budgetId = budgetData.id,
            )
            ?.let { (real, draft) ->
                val realAccount = real.toRealAccount()!!
                budgetData.addRealAccount(realAccount)
                budgetData.addDraftAccount(draft.toDraftAccount { if (it == realAccount.id) realAccount else null }!!)
                realAccount
            }
    } else {
        accountDao.createRealAccountOrNull(
            name,
            description,
            budgetId = budgetData.id,
        )
            ?.toRealAccount()
            ?.also {
                budgetData.addRealAccount(it)
            }
    }
        ?.also { realAccount: RealAccount ->
            createAndSaveIncomeTransaction(balance, realAccount, budgetData, clock, transactionDao, accountDao)
        }
        ?: run {
            outPrinter.important("Unable to save real account.")
            null
        }

private fun WithIo.createAndSaveIncomeTransaction(
    balance: BigDecimal,
    realAccount: RealAccount,
    budgetData: BudgetData,
    clock: Clock,
    transactionDao: TransactionDao,
    accountDao: AccountDao,
) {
    if (balance > BigDecimal.ZERO) {
        val incomeDescription: String =
            SimplePromptWithDefault(
                "Enter DESCRIPTION of income [initial balance in '${realAccount.name}']: ",
                defaultValue = "initial balance in '${realAccount.name}'",
                inputReader = inputReader,
                outPrinter = outPrinter,
            )
                .getResult()
            // NOTE I don't think this is possible when there's a default String value
                ?: throw Error("No description entered.")
        outPrinter("Enter timestamp for '$incomeDescription' transaction\n")
        val timestamp: Instant = getTimestampFromUser(
            timeZone = budgetData.timeZone,
            clock = clock,
        )
            ?.toInstant(budgetData.timeZone)
            ?: throw TryAgainAtMostRecentMenuException("No timestamp entered.")
        createInitialBalanceTransaction(
            incomeDescription,
            timestamp,
            balance,
            budgetData,
            realAccount,
        )
            .let { incomeTransaction: Transaction ->
                commitTransactionConsistently(incomeTransaction, transactionDao, accountDao, budgetData)
                outPrinter.important("Real account '${realAccount.name}' created with balance $$balance")
            }
    }
}

fun createChargeAccountConsistently(
    name: String,
    description: String,
    accountDao: AccountDao,
    budgetData: BudgetData,
): ChargeAccount? =
    accountDao
        .createChargeAccountOrNull(name, description, budgetId = budgetData.id)
        ?.toChargeAccount()
        ?.also { chargeAccount: ChargeAccount ->
            budgetData.addChargeAccount(chargeAccount)
        }

