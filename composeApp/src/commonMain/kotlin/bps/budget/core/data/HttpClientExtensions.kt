package bps.budget.core.data

import bps.budget.core.domain.DataError
import bps.budget.core.domain.Result
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

suspend inline fun <reified T> responseToResult(
    response: HttpResponse,
): Result<T, DataError.Remote> {
    return when (response.status.value) {
        in 200..299 -> {
            try {
                Result.Success(response.body<T>())
            } catch (_: NoTransformationFoundException) {
                Result.Error(DataError.Remote.SERIALIZATION)
            }
        }
        400 -> Result.Error(DataError.Remote.BAD_REQUEST)
        401 -> Result.Error(DataError.Remote.UNAUTHORIZED)
        403 -> Result.Error(DataError.Remote.FORBIDDEN)
        404 -> {
            Result.Error(DataError.Remote.NOT_FOUND)
        }
        405 -> Result.Error(DataError.Remote.METHOD_NOT_ALLOWED)
        406 -> Result.Error(DataError.Remote.NOT_ACCEPTABLE)
        408 -> Result.Error(DataError.Remote.REQUEST_TIMEOUT)
        409 -> {
            Result.Error(DataError.Remote.CONFLICT)
        }
        429 -> {
            Result.Error(DataError.Remote.TOO_MANY_REQUESTS)
        }
        in 500..599 -> {
            Result.Error(DataError.Remote.SERVER)
        }
        else -> {
            Result.Error(DataError.Remote.UNKNOWN)
        }
    }
}

suspend inline fun <reified T> safeCall(execute: () -> HttpResponse): Result<T, DataError.Remote> =
    try {
        responseToResult(execute())
    } catch (_: SocketTimeoutException) {
        Result.Error(DataError.Remote.REQUEST_TIMEOUT)
    } catch (_: UnresolvedAddressException) {
        Result.Error(DataError.Remote.NO_INTERNET)
    } catch (_: Exception) {
        coroutineContext.ensureActive()
        Result.Error(DataError.Remote.UNKNOWN)
    }
