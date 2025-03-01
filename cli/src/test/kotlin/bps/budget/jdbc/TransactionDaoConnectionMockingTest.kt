package bps.budget.jdbc

import bps.budget.jdbc.test.ConnectionMockingFixture
import bps.budget.model.CategoryAccount
import bps.budget.persistence.TransactionDao
import bps.budget.persistence.jdbc.JdbcTransactionDao
import bps.jdbc.JdbcConnectionProvider
import bps.jdbc.JdbcFixture
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@OptIn(ExperimentalUuidApi::class)
class TransactionDaoConnectionMockingTest : FreeSpec(), JdbcFixture, ConnectionMockingFixture {

    override val timestampFactory: () -> Timestamp = initializeTimestampFactory()
    override val jdbcConnectionProvider: JdbcConnectionProvider

    init {
        val (provider, connection) = mockConnectionProviderAndConnection()
        jdbcConnectionProvider = provider
        closeConnectionAfterSpec()

        val transactionDaoTestSubject: TransactionDao = JdbcTransactionDao(jdbcConnectionProvider)

        val budgetId: Uuid = Uuid.random()
        "test fetchTransactionItemsInvolvingAccount" {
            val preparedStatement = mockk<PreparedStatement>(relaxed = true)
            val resultSet = mockk<ResultSet>(relaxed = true)

            every { connection.prepareStatement(any()) } returns
                    preparedStatement
            every { preparedStatement.executeQuery() } answers {
                resultSet
            }

            every { resultSet.next() } returns
                    true andThen
                    true andThen
                    true andThen
                    false
            every { resultSet.getString(match<String> { it.endsWith("description") }) } returns
                    "description"
            every { resultSet.getString("draft_status") } returns
                    "none"
            every { resultSet.getString("type") } returns
                    "expense"
            every { resultSet.getObject(any<String>(), UUID::class.java) } answers {
                UUID.randomUUID()
            }
            every { resultSet.getObject("budget_id", UUID::class.java) } answers {
                budgetId.toJavaUuid()
            }
            every { resultSet.getTimestamp(any<String>()) } answers {
                timestampFactory()
            }
            val initialAmount = 5.toBigDecimal().setScale(2)
            var amount = initialAmount
            every { resultSet.getCurrencyAmount("amount") } returns
                    amount++ andThen
                    amount++ andThen
                    amount++ andThen
                    amount++ andThen
                    amount++ andThen
                    amount++ andThen
                    amount
            val account = CategoryAccount(
                name = "stuff",
                description = "stuff and things",
                id = Uuid.random(),
                balance = 1000.toBigDecimal().setScale(2),
                budgetId = budgetId,
            )
            val extendedTransactionItems =
                transactionDaoTestSubject
                    .fetchTransactionItemsInvolvingAccount(account, 3)
                    .sorted()
            withClue("extendedTransactionItems size") {
                extendedTransactionItems.size shouldBe 3
            }
            extendedTransactionItems[0]
                .asClue {
                    it.accountBalanceAfterItem shouldBe
                            1000.toBigDecimal().setScale(2)
                    it.amount shouldBe
                            5.toBigDecimal().setScale(2)
                }
            extendedTransactionItems[1]
                .asClue {
                    it.accountBalanceAfterItem shouldBe
                            995.toBigDecimal().setScale(2)
                    it.amount shouldBe
                            6.toBigDecimal().setScale(2)
                }
            extendedTransactionItems[2]
                .asClue {
                    it.accountBalanceAfterItem shouldBe
                            (995 - 6).toBigDecimal().setScale(2)
                    it.amount shouldBe
                            7.toBigDecimal().setScale(2)
                }
            every { resultSet.next() } returns
                    true andThen
                    true andThen
                    true andThen
                    false
            shouldThrow<IllegalArgumentException> {
                transactionDaoTestSubject
                    .fetchTransactionItemsInvolvingAccount(
                        account,
                        3,
                        3,
                    )
            }
            val secondPage =
                transactionDaoTestSubject
                    .fetchTransactionItemsInvolvingAccount(
                        account,
                        3,
                        3,
                        (995 - (6 + 7)).toBigDecimal().setScale(2),
                    )
                    .sorted()
            withClue("secondPage size") { secondPage.size shouldBe 3 }
            secondPage[0]
                .asClue {
                    it.accountBalanceAfterItem shouldBe
                            (995 - (6 + 7)).toBigDecimal().setScale(2)
                    it.amount shouldBe
                            8.toBigDecimal().setScale(2)
                }
            secondPage[1]
                .asClue {
                    it.accountBalanceAfterItem shouldBe
                            (995 - (6 + 7 + 8)).toBigDecimal().setScale(2)
                    it.amount shouldBe
                            9.toBigDecimal().setScale(2)
                }
            secondPage[2]
                .asClue {
                    it.accountBalanceAfterItem shouldBe
                            (995 - (6 + 7 + 8 + 9)).toBigDecimal().setScale(2)
                    it.amount shouldBe
                            10.toBigDecimal().setScale(2)
                }
        }

    }

}
