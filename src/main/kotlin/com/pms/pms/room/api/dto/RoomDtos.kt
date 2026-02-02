package com.pms.pms.room.api.dto

import io.swagger.v3.oas.annotations.media.Schema

data class RoomSummaryResponse(
    @field:Schema(example = "11111111-1111-1111-1111-111111111111")
    val roomId: String,

    @field:Schema(example = "22222222-2222-2222-2222-222222222222")
    val hotelId: String,

    @field:Schema(example = "deluxe")
    val roomType: String,

    val isActive: Boolean
)
