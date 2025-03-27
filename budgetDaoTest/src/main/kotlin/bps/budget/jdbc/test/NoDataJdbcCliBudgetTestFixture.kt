package bps.budget.jdbc.test

import bps.jdbc.JdbcConfig
import io.kotest.core.spec.Spec

interface NoDataJdbcCliBudgetTestFixture : JdbcCliBudgetTestFixture {

    fun Spec.dropAllBeforeEach() {
        beforeEach {
            dropTables(dataSource, jdbcConfig.schema)
        }
    }

    companion object {
        operator fun invoke(jdbcConfig: JdbcConfig, budgetName: String): NoDataJdbcCliBudgetTestFixture =
            object : NoDataJdbcCliBudgetTestFixture,
                JdbcCliBudgetTestFixture by JdbcCliBudgetTestFixture(
                    jdbcConfig,
                    budgetName,
                ) {}
    }

}
