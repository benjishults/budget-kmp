package bps.budget.server

import bps.budget.persistence.jdbc.JdbcConfig
import bps.config.ConfigurationHelper
import io.github.nhubbard.konf.Config
import io.github.nhubbard.konf.toValue

interface BudgetServerConfigurations {
    val jdbc: JdbcConfig
    val config: Config

    companion object {
        /**
         * Load configuration from the given files with the later files overriding the later.  Highest
         * precedence goes to env vars and system properties.
         */
        operator fun invoke(filePaths: Sequence<String>): BudgetServerConfigurations =
            object : BudgetServerConfigurations {
                override val config: Config =
                    ConfigurationHelper(filePaths)
                        .config
                override val jdbc: JdbcConfig =
                    config
                        .at("jdbc")
                        .toValue()

                override fun toString(): String =
                    "BudgetServerConfigurations($jdbc)"
            }
    }
}
