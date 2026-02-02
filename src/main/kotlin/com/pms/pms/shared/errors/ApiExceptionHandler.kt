package com.pms.pms.shared.errors

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ApiError(val code: String, val message: String)

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(NoAvailabilityException::class)
    fun noAvailability(ex: NoAvailabilityException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError("NO_AVAILABILITY", ex.message ?: "Conflict"))

    @ExceptionHandler(IdempotencyKeyConflictException::class)
    fun idempotencyConflict(ex: IdempotencyKeyConflictException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError("IDEMPOTENCY_KEY_CONFLICT", ex.message ?: "Conflict"))

    @ExceptionHandler(InvalidRequestException::class)
    fun invalidRequest(ex: InvalidRequestException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError("INVALID_REQUEST", ex.message ?: "Bad request"))
}
