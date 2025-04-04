package bps.budget.persistence

import bps.budget.model.User
import bps.budget.model.defaultGeneralAccountName
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface UserBudgetDao {

    fun getUserByLoginOrNull(login: String): User? =
        null

    fun createUser(
        login: String,
        password: String,
    ): UserEntity =
        TODO()

    /**
     * @return the budgetId
     */
    fun createBudget(
        generalAccountName: String = defaultGeneralAccountName,
//        budgetId: Uuid = Uuid.random(),
    ): BudgetEntity

    fun grantAccess(
        budgetName: String,
        timeZoneId: String,
        analyticsStart: Instant,
        userId: Uuid,
        budgetId: Uuid,
    )

    fun updateTimeZone(
        timeZoneId: String,
        userId: Uuid,
        budgetId: Uuid,
    )

    fun updateAnalyticsStart(
        analyticsStart: Instant,
        userId: Uuid,
        budgetId: Uuid,
    ): Int

    fun deleteBudget(budgetId: Uuid) {}
    fun deleteUser(userId: Uuid) {}
    fun deleteUserByLogin(login: String) {}
}
