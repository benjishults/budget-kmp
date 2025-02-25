package bps.budget.jdbc

import bps.budget.BudgetConfigurations
import io.kotest.core.spec.Spec

interface NoDataJdbcCliBudgetTestFixture : JdbcCliBudgetTestFixture {

    fun Spec.dropAllBeforeEach() {
        beforeEach {
            dropTables(connection, jdbcConfig.schema)
        }
    }

    companion object {
        operator fun invoke(): NoDataJdbcCliBudgetTestFixture =
            object : NoDataJdbcCliBudgetTestFixture,
                JdbcCliBudgetTestFixture by JdbcCliBudgetTestFixture(
                    BudgetConfigurations(sequenceOf("noDataJdbc.yml")).persistence.jdbc!!,
                    "clean after test",
                ) {}
    }

}
