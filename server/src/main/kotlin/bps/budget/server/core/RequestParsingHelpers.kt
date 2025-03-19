package bps.budget.server.core

import bps.kotlin.Result
import io.ktor.server.routing.RoutingCall
import kotlinx.coroutines.isActive
import kotlin.enums.enumEntries

/**
 * If [types] is empty, then everything is considered wanted.
 * @return the result of [producer] if [types] is empty or if [type] is in [types].  Otherwise, returns an empty [List].
 * @param producer will only be called if [types] is empty or if [type] is in [types].
 */
inline fun <W : Any, T : Any> ifTypeWanted(
    type: T,
    types: List<T>,
    producer: () -> List<W>,
): List<W> =
    if (types.isEmpty() || type in types)
        producer()
    else
        emptyList()

/**
 * @return `null` if any of the params passed in are not in the enum.  Otherwise, the list of [T]s that are query
 * parameters named [name].
 * @param name the name of the query parameter to collect enum values from.
 */
inline fun <reified T : Enum<T>> RoutingCall.extractQueryParamEnumValuesOrNull(
    name: String,
    errorMessage: () -> String = { "malformed value of query parameter: '$name'" },
): Result<List<T>, RequestError> =
    Result.Success(
        queryParameters
            .getAll(name)
            ?.map {
                try {
                    enumEntries<T>()
                        .first { it.name == name }
                } catch (e: Exception) {
                    if (coroutineContext.isActive) {
                        // NOTE non-local exit
                        return Result.Error(RequestError.BadQueryParameterValueError(errorMessage()))
                    } else {
                        throw e
                    }
                }
            }
            ?: emptyList(),
    )
