package com.pms.pms.room.application

import com.pms.pms.room.api.dto.RoomSummaryResponse
import com.pms.pms.room.infrastructure.RoomRepository
import com.pms.pms.shared.utils.parseUuidOrThrow
import org.springframework.stereotype.Service

@Service
class RoomService(
    private val repository: RoomRepository
) {
    suspend fun listByHotel(hotelId: String): List<RoomSummaryResponse> {
        val hotelUuid = parseUuidOrThrow(hotelId, "hotelId")
        return repository.findByHotelId(hotelUuid)
            .map { room ->
                RoomSummaryResponse(
                    roomId = room.id.toString(),
                    hotelId = room.hotelId.toString(),
                    roomType = room.roomType,
                    isActive = room.isActive
                )
            }
    }
}
