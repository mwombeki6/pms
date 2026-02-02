package com.pms.pms.room.domain

import java.util.UUID

data class Room(
    val id: UUID,
    val hotelId: UUID,
    val roomType: String,
    val isActive: Boolean
)
