package bps.budget.consistency

import bps.budget.model.BudgetData
import bps.budget.persistence.UserBudgetDao
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import java.util.UUID

/**
 * Updates the persistent data with the new time-zone and modifies the budgetData object if the data update
 * succeeded in updating at least one entity.
 * @return the number of persistent data entities updated
 */
fun updateTimeZoneConsistently(
    newTimeZone: TimeZone,
    userId: UUID,
    budgetId: UUID,
    userBudgetDao: UserBudgetDao,
    budgetData: BudgetData,
): Int =
    userBudgetDao.updateTimeZone(
        timeZoneId = newTimeZone.id,
        userId = userId,
        budgetId = budgetId,
    )
        .also {
            if (it >= 1)
                budgetData.timeZone = newTimeZone
        }

/**
 * Updates the persistent data with the new analyticsStart and modifies the budgetData object if the data update
 * succeeded in updating at least one entity.
 * @return the number of persistent data entities updated
 */
fun updateAnalyticsStartConsistently(
    newAnalyticsStart: Instant,
    userId: UUID,
    budgetId: UUID,
    userBudgetDao: UserBudgetDao,
    budgetData: BudgetData,
): Int =
    userBudgetDao.updateAnalyticsStart(
        userId = userId,
        analyticsStart = newAnalyticsStart,
        budgetId = budgetId,
    )
        .also {
            if (it >= 1)
                budgetData.analyticsStart = newAnalyticsStart
        }
