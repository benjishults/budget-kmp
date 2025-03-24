package bps.budget.persistence

import bps.budget.analytics.AnalyticsOptions
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface AnalyticsDao {

    val clock: Clock

    fun averageIncome(
        timeZone: TimeZone,
        options: AnalyticsOptions,
        budgetId: Uuid,
    ): BigDecimal? =
        TODO()

    fun averageIncome(
        realAccountId: Uuid,
        timeZone: TimeZone,
        options: AnalyticsOptions,
        budgetId: Uuid,
    ): BigDecimal? =
        TODO()

    fun maxIncome(): BigDecimal? =
        TODO("Not yet implemented")

    fun minIncome(): BigDecimal? =
        TODO("Not yet implemented")

    fun averageExpenditure(
        categoryAccountId: Uuid,
        timeZone: TimeZone,
        options: AnalyticsOptions,
        budgetId: Uuid,
    ): BigDecimal? =
        TODO()

    fun averageExpenditure(
        timeZone: TimeZone,
        options: AnalyticsOptions,
        budgetId: Uuid,
    ): BigDecimal? =
        TODO()

    fun maxExpenditure(): BigDecimal? =
        TODO("Not yet implemented")

    fun minExpenditure(): BigDecimal? =
        TODO("Not yet implemented")

}
