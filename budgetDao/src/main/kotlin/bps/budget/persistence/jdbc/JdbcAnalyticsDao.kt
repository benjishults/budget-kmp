package bps.budget.persistence.jdbc

import bps.budget.analytics.AnalyticsOptions
import bps.budget.persistence.AnalyticsDao
import bps.jdbc.JdbcFixture
import bps.jdbc.JdbcFixture.Companion.transactOrThrow
import bps.time.NaturalLocalInterval
import bps.time.atStartOfMonth
import bps.time.naturalMonthInterval
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.SortedMap
import java.util.TreeMap
import javax.sql.DataSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class JdbcAnalyticsDao(
    val dataSource: DataSource,
    override val clock: Clock = Clock.System,
) : AnalyticsDao, JdbcFixture {

    data class Item(
        val amount: BigDecimal,
        val timestamp: LocalDateTime,
    ) {
        init {
            require(amount > BigDecimal.ZERO) { "amount must be positive" }
        }
    }

    class ItemsInInterval(
        val interval: NaturalLocalInterval,
    ) : Comparable<ItemsInInterval> {

        private val _items = mutableListOf<Item>()
        val items: List<Item>
            get() = _items.toList()

        fun add(item: Item) {
            _items.add(item)
        }

        override fun compareTo(other: ItemsInInterval): Int =
            interval.start.compareTo(other.interval.start)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ItemsInInterval) return false

            if (interval != other.interval) return false

            return true
        }

        override fun hashCode(): Int {
            return interval.hashCode()
        }

    }

    open class ItemSeries<K : Comparable<K>>(
        open val items: SortedMap<K, ItemsInInterval>,
    ) {
//        init {
//            require(
//                expenditures
//                    .keys
//                    .asSequence()
//                    .drop(1)
//                    .runningFold(expenditures.keys.first()) { a: NaturalLocalInterval, b: NaturalLocalInterval ->
//                        a.start + a.unit == b.start
//                    }
//                    .all { it },
//            )
//        }
    }

    class MonthlyItemSeries(
        items: SortedMap<LocalDateTime, ItemsInInterval> = TreeMap(),
    ) : ItemSeries<LocalDateTime>(items) {
//        init {
//            var previous: NaturalMonthLocalInterval = expenditures.keys.first()
//            require(
//                expenditures
//                    .keys
//                    .asSequence()
//                    .drop(1)
//                    .map { current: NaturalMonthLocalInterval ->
//                        (previous.start.monthNumber % 12 == current.start.monthNumber - 1)
//                            .also {
//                                previous = current
//                            }
//                    }
//                    .all { it },
//            )
//        }

        // TODO this would be less error-prone if we used Instants instead of LocalDateTimes because the start of the month
        //      is usually not an ambiguous LocalDateTime and it would be the only one we would need to do translation on.
        fun add(item: Item) {
            this@MonthlyItemSeries.items.compute(item.timestamp.atStartOfMonth()) { key: LocalDateTime, foundValue: ItemsInInterval? ->
                (foundValue ?: ItemsInInterval(key.naturalMonthInterval()))
                    .also {
                        it.add(item)
                    }
            }
        }

        // TODO worry about options
        // TODO worry about exceptions
        fun average(options: AnalyticsOptions): BigDecimal? =
            computeMonthlyTotals(options)
                .let { listOfTotals: List<BigDecimal> ->
                    listOfTotals
                        .reduceOrNull(BigDecimal::plus)
                        ?.setScale(2, RoundingMode.HALF_EVEN)
                        ?.divide(listOfTotals.size.toBigDecimal(), 2, RoundingMode.HALF_EVEN)
                }

        // TODO worry about options
        // TODO worry about exceptions
        fun max(options: AnalyticsOptions): BigDecimal? =
            computeMonthlyTotals(options)
                .let { listOfTotals: List<BigDecimal> ->
                    if (listOfTotals.isEmpty())
                        null
                    else
                        listOfTotals.max()
                }

        // TODO worry about options
        // TODO worry about exceptions
        fun min(options: AnalyticsOptions): BigDecimal? =
            computeMonthlyTotals(options)
                .let { listOfTotals: List<BigDecimal> ->
                    if (listOfTotals.isEmpty())
                        null
                    else
                        listOfTotals.min()
                }

        /**
         * @return a [List] of [BigDecimal]s whose length is the total number of months specified by [options].
         * The elements of that [List] are, in order of months ascending, the average amounts of the month.
         * If a month has no transactions, the value will be [BigDecimal.ZERO].
         */
        // FIXME gh-76 this still starts too late and stops too early according to [options].
        //       We need to compute the start and end interval before starting anything else.
        // TODO worry about options
        // TODO worry about exceptions
        private fun computeMonthlyTotals(options: AnalyticsOptions): List<BigDecimal> =
            if (this@MonthlyItemSeries.items.isEmpty())
                emptyList()
            else {
                var runningTransactionIntervalStart: LocalDateTime =
                    this@MonthlyItemSeries
                        .items
                        .firstKey()
                this@MonthlyItemSeries
                    .items
                    .map { (intervalStart: LocalDateTime, v: ItemsInInterval) ->
                        if (intervalStart != runningTransactionIntervalStart) {
                            BigDecimal.ZERO.setScale(2)
                        } else {
                            v.items
                                .map { it.amount }
                                .reduce(BigDecimal::plus)
                        }
                            .also {
                                runningTransactionIntervalStart =
                                    runningTransactionIntervalStart
                                        .month
                                        .takeIf { it == Month.DECEMBER }
                                        ?.let {
                                            LocalDateTime(
                                                year = runningTransactionIntervalStart.year + 1,
                                                month = Month.JANUARY,
                                                dayOfMonth = 1,
                                                hour = 0,
                                                minute = 0,
                                                second = 0,
                                            )
                                        }
                                        ?: LocalDateTime(
                                            year = runningTransactionIntervalStart.year,
                                            month = runningTransactionIntervalStart.month + 1,
                                            dayOfMonth = 1,
                                            hour = 0,
                                            minute = 0,
                                            second = 0,
                                        )
                            }
                    }
            }

    }

    // FIXME
    override fun maxIncome(): BigDecimal? =
        null

    // FIXME
    override fun minIncome(): BigDecimal? =
        null

    override fun averageIncome(
        timeZone: TimeZone,
        options: AnalyticsOptions,
        budgetId: Uuid,
    ): BigDecimal? =
        MonthlyItemSeries()
            .let { incomes: MonthlyItemSeries ->
                dataSource.transactOrThrow {
                    prepareStatement(
                        """
                |select t.timestamp_utc, i.amount
                |from transaction_items i
                |join transactions t
                |    on i.transaction_id = t.id
                |    and i.budget_id = t.budget_id
                |join accounts a
                |    on i.account_id = a.id
                |    and i.budget_id = a.budget_id
                |where t.type = 'income'
                |  and i.amount > 0
                |  and a.type in ('real', 'charge')
                |  and t.timestamp_utc >= ?
                |  ${if (options.endDateLimited) "and t.timestamp_utc < ?" else ""}
                |  and i.budget_id = ?
                |order by t.timestamp_utc asc
            """.trimMargin(),
                        // TODO page this if we run into DB latency
//                |offset ?
//                |limit 100
                    )
                        .use { statement: PreparedStatement ->
                            statement.setInstant(1, options.since)
                            statement.setUuid(
                                setEndTimeStampMaybe(
                                    options,
                                    statement,
                                    2,
                                    this@JdbcAnalyticsDao.clock.now(),
                                    timeZone,
                                ),
                                budgetId,
                            )
                            statement.executeQuery()
                                .use { resultSet: ResultSet ->
                                    while (resultSet.next()) {
                                        incomes.add(
                                            Item(
                                                resultSet.getBigDecimal("amount"),
                                                resultSet.getInstantOrNull()!!
                                                    // FIXME do these really need to be LocalDateTimes?
                                                    //       if not, we may avoid some problems my leaving them as Instants
                                                    .toLocalDateTime(timeZone),
                                            ),
                                        )
                                    }
                                }
                        }
                }
                incomes.average(options)
            }

    override fun averageIncome(
        realAccountId: Uuid,
        timeZone: TimeZone,
        options: AnalyticsOptions,
        budgetId: Uuid,
    ): BigDecimal? =
        dataSource.transactOrThrow {
            val incomes = MonthlyItemSeries()
            prepareStatement(
                """
                |select t.timestamp_utc, ti.amount from transaction_items ti
                |join transactions t
                |  on ti.transaction_id = t.id
                |    and ti.budget_id = t.budget_id
                |where ti.account_id = ?
                |  and t.type = 'income'
                |  and ti.amount > 0
                |  and t.timestamp_utc >= ?
                |  ${if (options.endDateLimited) "and t.timestamp_utc < ?" else ""}
                |  and ti.budget_id = ?
                |order by t.timestamp_utc asc
            """.trimMargin(),
                // TODO page this if we run into DB latency
//                |offset ?
//                |limit 100
            )
                .use { statement: PreparedStatement ->
                    statement.setUuid(1, realAccountId)
                    statement.setInstant(2, options.since)
                    statement.setUuid(
                        setEndTimeStampMaybe(options, statement, 3, this@JdbcAnalyticsDao.clock.now(), timeZone),
                        budgetId,
                    )
                    statement.executeQuery()
                        .use { resultSet: ResultSet ->
                            while (resultSet.next()) {
                                incomes.add(
                                    Item(
                                        resultSet.getBigDecimal("amount"),
                                        resultSet.getInstantOrNull()!!
                                            // FIXME do these really need to be LocalDateTimes?
                                            //       if not, we may avoid some problems my leaving them as Instants
                                            .toLocalDateTime(timeZone),
                                    ),
                                )
                            }
                        }
                }
            incomes.average(options)
        }

    override fun averageExpenditure(
        timeZone: TimeZone,
        options: AnalyticsOptions,
        budgetId: Uuid,
    ): BigDecimal? =
        dataSource.transactOrThrow {
            val expenditures = MonthlyItemSeries()
            prepareStatement(
                """
                |select t.timestamp_utc, i.amount
                |from transaction_items i
                |join transactions t
                |    on i.transaction_id = t.id
                |    and i.budget_id = t.budget_id
                |join accounts a
                |    on i.account_id = a.id
                |    and i.budget_id = a.budget_id
                |where t.type = 'expense'
                |  and i.amount < 0
                |  and a.type = 'category'
                |  and t.timestamp_utc >= ?
                |  ${if (options.endDateLimited) "and t.timestamp_utc < ?" else ""}
                |  and i.budget_id = ?
                |order by t.timestamp_utc asc
            """.trimMargin(),
                // TODO page this if we run into DB latency
//                |offset ?
//                |limit 100
            )
                .use { statement: PreparedStatement ->
                    statement.setInstant(1, options.since)
                    statement.setUuid(
                        setEndTimeStampMaybe(options, statement, 2, this@JdbcAnalyticsDao.clock.now(), timeZone),
                        budgetId,
                    )
                    statement.executeQuery()
                        .use { resultSet: ResultSet ->
                            while (resultSet.next()) {
                                expenditures.add(
                                    Item(
                                        -resultSet.getBigDecimal("amount"),
                                        resultSet.getInstantOrNull()!!
                                            // FIXME do these really need to be LocalDateTimes?
                                            //       if not, we may avoid some problems my leaving them as Instants
                                            .toLocalDateTime(timeZone),
                                    ),
                                )
                            }
                        }
                }
            expenditures.average(options)
        }


    // TODO make a single function that returns various analytics to avoid multiple trips to the DB.
    //      I imagine each of these analytics functions will be pulling the same data.
    override fun averageExpenditure(
        categoryAccountId: Uuid,
        timeZone: TimeZone,
        options: AnalyticsOptions,
        budgetId: Uuid,
    ): BigDecimal? =
        dataSource.transactOrThrow {
            val expenditures = MonthlyItemSeries()
            prepareStatement(
                """
                |select t.timestamp_utc, ti.amount
                |from transaction_items ti
                |  join transactions t
                |    on ti.transaction_id = t.id
                |      and ti.budget_id = t.budget_id
                |where ti.account_id = ?
                |  and t.type = 'expense'
                |  and ti.amount < 0
                |  and t.timestamp_utc >= ?
                |  ${if (options.endDateLimited) "and t.timestamp_utc < ?" else ""}
                |  and ti.budget_id = ?
                |order by t.timestamp_utc asc
            """.trimMargin(),
                // TODO page this if we run into DB latency
//                |offset ?
//                |limit 100
            )
                .use { statement: PreparedStatement ->
                    statement.setUuid(1, categoryAccountId)
                    statement.setInstant(2, options.since)
                    statement.setUuid(
                        setEndTimeStampMaybe(options, statement, 3, this@JdbcAnalyticsDao.clock.now(), timeZone),
                        budgetId,
                    )
                    statement
                        .executeQuery()
                        .use { resultSet: ResultSet ->
                            while (resultSet.next()) {
                                expenditures.add(
                                    Item(
                                        -resultSet.getBigDecimal("amount"),
                                        resultSet.getInstantOrNull()!!
                                            // FIXME do these really need to be LocalDateTimes?
                                            //       if not, we may avoid some problems by leaving them as Instants
                                            .toLocalDateTime(timeZone),
                                    ),
                                )
                            }
                        }
                }
            expenditures.average(options)
        }

    // FIXME
    override fun maxExpenditure(): BigDecimal? =
        null

    // FIXME
    override fun minExpenditure(): BigDecimal? =
        null

    /**
     * Fills in the appropriate [Timestamp] at [statement]'s placeholder [atIndex].  If it filled anything in there,
     * it will return [atIndex]` + 1`.  Otherwise, it returns [atIndex].
     */
    private fun setEndTimeStampMaybe(
        options: AnalyticsOptions,
        statement: PreparedStatement,
        atIndex: Int,
        now: Instant,
        timeZone: TimeZone,
    ): Int =
        if (options.excludePreviousUnit) {
            statement.setInstant(
                atIndex,
                now
                    .atStartOfMonth(timeZone)
                    .let {
                        if (it.month === Month.JANUARY)
                            LocalDateTime(it.year - 1, 12, 1, 0, 0)
                        else
                            LocalDateTime(it.year, it.monthNumber - 1, 1, 0, 0)
                    }
                    // NOTE safe because it is midnight and offsets generally occur at or after
                    //      1 a.m. local time
                    .toInstant(timeZone),
            )
            atIndex + 1
        } else if (options.excludeCurrentUnit) {
            statement.setInstant(
                atIndex,
                now
                    .atStartOfMonth(timeZone)
                    // NOTE safe because it is midnight and offsets generally occur at or after
                    //      1 a.m. local time
                    .toInstant(timeZone),
            )
            atIndex + 1
        } else if (options.excludeFutureTransactions) {
            statement.setInstant(2, now)
            atIndex + 1
        } else {
            atIndex
        }

}
