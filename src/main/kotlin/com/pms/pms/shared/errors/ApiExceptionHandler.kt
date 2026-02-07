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

    @ExceptionHandler(HoldNotFoundException::class)
    fun holdNotFound(ex: HoldNotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiError("HOLD_NOT_FOUND", ex.message ?: "Not found"))

    @ExceptionHandler(HoldExpiredException::class)
    fun holdExpired(ex: HoldExpiredException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError("HOLD_EXPIRED", ex.message ?: "Conflict"))

    @ExceptionHandler(HoldStatusConflictException::class)
    fun holdStatusConflict(ex: HoldStatusConflictException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiError("HOLD_STATUS_CONFLICT", ex.message ?: "Conflict"))
}
