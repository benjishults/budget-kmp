package bps.budget.persistence

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class UserEntity(
    val userId: Uuid,
    val userName: String,
)
