package bps.budget.model

import kotlinx.serialization.Serializable

@Serializable
class Links(
    val next: String? = null,
    val previous: String? = null,
)
