package bps.budget.jdbc.test

import io.kotest.core.spec.Spec
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource

interface ConnectionMockingFixture {
    // TODO separate this into another fixture
    val timestampFactory: () -> Timestamp

    val dataSource: DataSource

    /**
     * After the [io.kotest.core.spec.Spec] is done, this will call [io.mockk.unmockkAll].
     */
    fun Spec.closeConnectionAfterSpec() {
        afterSpec {
            unmockkAll()
        }
    }

    fun mockDataSourceAndConnection(block: Connection.() -> Unit = {}): Pair<DataSource, Connection> =
        (mockk<DataSource>(relaxed = true) to mockDriverManagerAndConnection())
            .also { (provider, connection) ->
                every { provider.connection } returns connection
            }

    fun initializeTimestampFactory(atMinute: String = "2024-08-09T00:00"): () -> Timestamp =
        object : () -> Timestamp {
            var secondCount = 0

            @Suppress("DefaultLocale")
            override operator fun invoke(): Timestamp =
                Timestamp.from(Instant.parse(String.format("$atMinute:%02d.500Z", secondCount++)))
        }

    private fun mockDriverManagerAndConnection(): Connection {
        mockkStatic(DriverManager::class)
        val connection = mockk<Connection>(relaxed = true)
        every {
            DriverManager.getConnection(any(), any(), any())
        } returns connection
        every { connection.isValid(any()) } returns true
        return connection
    }

}
