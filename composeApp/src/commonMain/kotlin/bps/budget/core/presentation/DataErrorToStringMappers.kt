package bps.budget.core.presentation

import bps.budget.core.domain.DataError

fun DataError.toUiText(): UiText =
    when (this) {
        // TODO use string resources for localization
        DataError.Remote.REQUEST_TIMEOUT -> UiText.DynamicString("Request timeout")
        DataError.Remote.TOO_MANY_REQUESTS -> UiText.DynamicString("Too Many Requests")
        DataError.Remote.NO_INTERNET -> UiText.DynamicString("No Internet")
        DataError.Remote.SERVER -> UiText.DynamicString("Server Error")
        DataError.Remote.NOT_FOUND -> UiText.DynamicString("Not found")
        DataError.Remote.BAD_REQUEST -> UiText.DynamicString("Bad Request")
        DataError.Remote.UNAUTHORIZED -> UiText.DynamicString("Unauthorized")
        DataError.Remote.FORBIDDEN -> UiText.DynamicString("Forbidden")
        DataError.Remote.METHOD_NOT_ALLOWED -> UiText.DynamicString("Method Not Allowed")
        DataError.Remote.NOT_ACCEPTABLE -> UiText.DynamicString("Not Acceptable Content Type")
        DataError.Remote.CONFLICT -> UiText.DynamicString("Conflict")
        DataError.Remote.SERIALIZATION -> UiText.DynamicString("Serialization Problem")
        DataError.Remote.UNKNOWN -> UiText.DynamicString("Unknown Error")
        DataError.Local.DISK_FULL -> UiText.DynamicString("Disk Full!")
        DataError.Local.UNKNOWN -> UiText.DynamicString("Unknown Error")
    }
