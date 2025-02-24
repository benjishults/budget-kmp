package bps.budget

import bps.budget.persistence.jdbc.JdbcConfig
import org.apache.commons.validator.routines.EmailValidator

data class PersistenceConfiguration(
    val type: String,
//    val file: FileConfig? = null,
    val jdbc: JdbcConfig? = null,
)

data class UserConfiguration(
    val defaultLogin: String? = null,
    val numberOfItemsInScrollingList: Int = 30,
) {
    init {
        require(
            defaultLogin === null ||
                    EmailValidator.getInstance().isValid(defaultLogin),
        ) { "defaultLogin, if provided, must be a valid email address.  Was '$defaultLogin'" }
    }
}

fun interface ConfigSelector : (PersistenceConfiguration) -> JdbcConfig
fun interface DaoBuilder : (JdbcConfig, String) -> BudgetDao

fun buildBudgetDao(configurations: PersistenceConfiguration, budgetName: String): BudgetDao =
    budgetDataBuilderMap[configurations.type]!!
        .let { (configSelector: ConfigSelector, daoBuilder: DaoBuilder) ->
            daoBuilder(configSelector(configurations), budgetName)
        }

val budgetDataBuilderMap: Map<String, Pair<ConfigSelector, DaoBuilder>> =
    mapOf(
        "JDBC" to (ConfigSelector { it.jdbc!! } to DaoBuilder { config, budgetName -> JdbcDao(config, budgetName) }),
//        "FILE" to (ConfigSelector { it.file!! } to DaoBuilder { FilesDao(it as FileConfig) }),
    )

//fun getBudgetNameFromPersistenceConfig(configuration: PersistenceConfiguration) =
//    budgetDataBuilderMap[configuration.type]!!
//        .first(configuration)
//        .budgetName
