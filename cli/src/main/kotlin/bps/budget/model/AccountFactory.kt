package bps.budget.model

import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface AccountFactory<out A : Account> : (String, String, Uuid, BigDecimal, Uuid, Uuid?) -> A {
    val type: AccountType
}
