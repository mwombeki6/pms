package com.pms.pms.shared.errors

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ApiError(val code: String, val message: String)

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(NoAvailabilityException::class)
    fun noAvailability(ex: NoAvailabilityException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError("NO_AVAILABILITY", ex.message ?: "Conflict"))
}