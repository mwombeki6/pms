package com.pms.pms.shared.utils

import com.pms.pms.shared.errors.InvalidRequestException
import java.util.UUID

fun parseUuidOrThrow(value: String, field: String): UUID =
    try {
        UUID.fromString(value)
    } catch (_ex: IllegalArgumentException) {
        throw InvalidRequestException("$field must be a valid UUID")
    }
