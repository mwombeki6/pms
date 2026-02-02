package com.pms.pms.api.v1.reservations.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

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

    @field:NotBlank
    @field:Schema(example = "2026-02-01 12:00:00")
    val checkIn: String,

    @field:NotBlank
    @field:Schema(example = "2026-02-03 10:00:00")
    val checkOut: String,
)

data class CreateHoldResponse(
    val holdId: String,
    val roomId: String,
    val status: String,
    val expiresAt: String,
)
