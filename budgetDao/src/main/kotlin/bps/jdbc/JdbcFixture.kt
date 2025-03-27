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
import javax.sql.DataSource
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
         * @returns the result of [block] on success.
         */
        inline fun <T> DataSource.transactOrThrow(
            block: Connection.() -> T,
        ): T =
            connection
                .use { connection: Connection ->
                    var error: Boolean = false
                    try {
                        connection.block()
                    } catch (t: Throwable) {
                        try {
                            error = true
                            connection.rollback()
                            throw t
                        } catch (rollbackException: Exception) {
                            rollbackException.addSuppressed(t)
                            throw rollbackException
                        }
                    } finally {
                        // NOTE this is odd but needed since [block] is inlined and a non-local return can happen there.
                        if (!error) {
                            connection.commit()
                        }
                    }
                }

    }
}
