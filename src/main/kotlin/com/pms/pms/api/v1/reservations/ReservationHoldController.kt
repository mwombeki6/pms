package com.pms.pms.api.v1.reservations

import com.pms.pms.api.v1.reservations.dto.CreateHoldRequest
import com.pms.pms.api.v1.reservations.dto.CreateHoldResponse
import com.pms.pms.booking.application.ReservationHoldService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/reservations/holds")
class ReservationHoldController (
    private val service: ReservationHoldService
) {
    @Operation(
        summary = "Create a new reservation hold",
        description = "Create a new temporary reservation hold to prevent double-booking while payment is in progress",
        responses = [
            ApiResponse(responseCode = "201", description = "Hold created Successfully"),
            ApiResponse(responseCode = "400", description = "Invalid request"),
            ApiResponse(responseCode = "409", description = "No availability or idempotency conflict")
        ]
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createHold(
        @Parameter(
            required = true,
            description = "Idempotency key. Same key + same request returns same response.",
            schema = Schema(type = "string", example = "0f1c2d3e-4a5b-6789-aaaa-bbbbccccdddd")
        )
        @RequestHeader("Idempotency-key") idemKey: String,

        @Valid @RequestBody req: CreateHoldRequest
    ): CreateHoldResponse = service.createHold(idemKey, req)
}
