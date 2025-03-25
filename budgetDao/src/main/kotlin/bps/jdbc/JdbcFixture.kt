package bps.jdbc

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
interface JdbcFixture {

    fun PreparedStatement.setInstant(parameterIndex: Int, timestamp: Instant) {
        setTimestamp(parameterIndex, Timestamp(timestamp.toEpochMilliseconds()))
    }

    /**
     * This assumes that the DB [Timestamp] is stored in UTC.
     */
    fun Timestamp.toLocalDateTime(timeZone: TimeZone): LocalDateTime =
        this
            .toInstant()
            .toKotlinInstant()
            .toLocalDateTime(timeZone)

    fun ResultSet.getLocalDateTimeForTimeZone(
        timeZone: TimeZone,
        columnLabel: String = "timestamp_utc",
    ): LocalDateTime =
        getTimestamp(columnLabel)
            .toLocalDateTime(timeZone)

    fun ResultSet.getInstant(columnLabel: String = "timestamp_utc"): Instant =
        getTimestamp(columnLabel)
            .toInstant()
            .toKotlinInstant()

    fun ResultSet.getInstantOrNull(columnLabel: String = "timestamp_utc"): Instant? =
        getTimestamp(columnLabel)
            ?.toInstant()
            ?.toKotlinInstant()

    fun ResultSet.getCurrencyAmount(name: String): BigDecimal =
        getBigDecimal(name).setScale(2)

    fun ResultSet.getUuid(name: String): Uuid? =
        getObject(name, UUID::class.java)
            ?.toKotlinUuid()

    fun PreparedStatement.setUuid(parameterIndex: Int, uuid: Uuid) =
        setObject(parameterIndex, uuid, Types.OTHER)

    companion object : JdbcFixture {
        /**
         * Commits after running [block].  Throws and rolls back if [block] or [Connection.commit] throw.
         */
        @JvmStatic
        inline fun <T> Connection.transactOrThrow(
            block: Connection.() -> T,
        ): T =
            transact({ throw it }, block)

        /**
         * Commits after running [block].  On any [Throwable], rolls back and returns the result of [onRollback].
         * @returns the result of [block] on success or the value of executing [onRollback] if anything is thrown.
         * @param onRollback defaults to throwing the [Throwable].  Called only if a [Throwable] is thrown from [block]
         * or from the subsequent call to [Connection.commit].
         */
        @JvmStatic
        inline fun <T> Connection.transact(
            onRollback: (Throwable) -> T = { throw it },
            block: Connection.() -> T,
        ): T =
            try {
                block()
                    .also {
                        commit()
                    }
            } catch (throwable: Throwable) {
                try {
                    rollback()
                    onRollback(throwable)
                } catch (rollbackException: Exception) {
                    rollbackException.addSuppressed(throwable)
                    throw rollbackException
                }
            }
    }

}
