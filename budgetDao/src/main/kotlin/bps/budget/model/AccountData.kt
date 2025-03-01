package bps.budget.model

import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// TODO needed?
@OptIn(ExperimentalUuidApi::class)
interface AccountData {
    val name: String
    val id: Uuid
    val description: String
    val balance: BigDecimal
}
