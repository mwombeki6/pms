package com.pms.pms.api.v1.reservations.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime

data class CreateHoldRequest(
    @field:NotBlank
    @field:Schema(example = "11111111-1111-1111-1111")
    val hotelId: String,

    @field:NotBlank
    @field:Schema(example = "Mwombeki Lubere")
    val guestName: String,

    @field:NotBlank
    @field:Schema(example = "+255748051333")
    val guestPhone: String,

    @field:NotNull
    @field:Schema(type = "string", format = "date-time", example = "2026-02-01T12:00:00Z")
    val checkIn: OffsetDateTime,

    @field:NotNull
    @field:Schema(type = "string", format = "date-time", example = "2026-02-03T10:00:00Z")
    val checkOut: OffsetDateTime,
) {
    @Suppress("unused")
    @get:AssertTrue(message = "checkOut must be after checkIn")
    @get:JsonIgnore
    val isStayRangeValid: Boolean
        get() = checkOut.isAfter(checkIn)
}

data class CreateHoldResponse(
    val holdId: String,
    val roomId: String,
    val status: String,
    val expiresAt: String,
)
