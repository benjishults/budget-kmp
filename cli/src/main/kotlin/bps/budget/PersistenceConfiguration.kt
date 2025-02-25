package bps.budget

import bps.jdbc.JdbcConfig
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
