@file:OptIn(ExperimentalUuidApi::class)

package bps.budget.app

import androidx.core.bundle.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed interface Route {
    @Serializable
    data object AccountGraph : Route

    @Serializable
    data object AccountBalanceList : Route

    @Serializable
    data class AccountDetail(val id: Uuid) : Route
}

// TODO get rid of this once navigation supports Kotlin 2.1
object UuidNavType : NavType<Uuid>(isNullableAllowed = true) {
    override fun get(bundle: Bundle, key: String): Uuid? =
        bundle
            .getString(key)
            ?.let { Uuid.parse(it) }

    override fun parseValue(value: String): Uuid =
        Uuid.parse(value)

    override fun put(bundle: Bundle, key: String, value: Uuid) =
        bundle.putString(key, value.toString())
}
