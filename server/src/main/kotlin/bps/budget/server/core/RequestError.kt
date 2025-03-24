package bps.budget.server.core

import bps.kotlin.Error

sealed interface RequestError : Error {

    val message: String

    data class BadQueryParameterValueError(
        override val message: String = "malformed value of query parameter",
    ) : RequestError

}
