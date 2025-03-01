package bps.budget.persistence

import bps.budget.model.User
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface UserBudgetDao {

    fun getUserByLoginOrNull(login: String): User? = null

    fun createUser(
        login: String,
        password: String,
        userId: Uuid = Uuid.random(),
    ): Uuid = TODO()

    fun createBudgetOrNull(
        generalAccountId: Uuid,
        budgetId: Uuid = Uuid.random(),
    ): Uuid?

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
    ): Int

    fun updateAnalyticsStart(
        analyticsStart: Instant,
        userId: Uuid,
        budgetId: Uuid,
    ): Int

    fun deleteBudget(budgetId: Uuid) {}
    fun deleteUser(userId: Uuid) {}
    fun deleteUserByLogin(login: String) {}
}
