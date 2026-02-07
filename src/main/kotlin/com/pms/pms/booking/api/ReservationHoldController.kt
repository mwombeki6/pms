package com.pms.pms.booking.api

import com.pms.pms.booking.api.dto.CreateHoldRequest
import com.pms.pms.booking.api.dto.CreateHoldResponse
import com.pms.pms.booking.api.dto.HoldResponse
import com.pms.pms.booking.application.ReservationHoldService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
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

    @Operation(
        summary = "Confirm a reservation hold",
        description = "Confirm a hold before it expires",
        responses = [
            ApiResponse(responseCode = "200", description = "Hold confirmed"),
            ApiResponse(responseCode = "404", description = "Hold not found"),
            ApiResponse(responseCode = "409", description = "Hold expired or status conflict")
        ]
    )
    @PostMapping("/{holdId}/confirm")
    suspend fun confirmHold(
        @Parameter(
            required = true,
            description = "Hold UUID",
            schema = Schema(type = "string", example = "0f1c2d3e-4a5b-6789-aaaa-bbbbccccdddd")
        )
        @PathVariable("holdId") holdId: String
    ): HoldResponse = service.confirmHold(holdId)

    @Operation(
        summary = "Cancel a reservation hold",
        description = "Cancel a hold or confirmed reservation",
        responses = [
            ApiResponse(responseCode = "200", description = "Hold cancelled"),
            ApiResponse(responseCode = "404", description = "Hold not found"),
            ApiResponse(responseCode = "409", description = "Hold expired or status conflict")
        ]
    )
    @PostMapping("/{holdId}/cancel")
    suspend fun cancelHold(
        @Parameter(
            required = true,
            description = "Hold UUID",
            schema = Schema(type = "string", example = "0f1c2d3e-4a5b-6789-aaaa-bbbbccccdddd")
        )
        @PathVariable("holdId") holdId: String
    ): HoldResponse = service.cancelHold(holdId)
}
