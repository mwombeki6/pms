package com.pms.pms.room.api

import com.pms.pms.room.api.dto.RoomSummaryResponse
import com.pms.pms.room.application.RoomService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/rooms")
class RoomController(
    private val service: RoomService
) {
    @Operation(
        summary = "List rooms for a hotel",
        description = "Fetch rooms by hotel id",
        responses = [
            ApiResponse(responseCode = "200", description = "Rooms returned"),
            ApiResponse(responseCode = "400", description = "Invalid request")
        ]
    )
    @GetMapping
    suspend fun listRooms(
        @Parameter(
            description = "Hotel UUID",
            required = true,
            example = "22222222-2222-2222-2222-222222222222"
        )
        @RequestParam("hotelId") hotelId: String
    ): List<RoomSummaryResponse> = service.listByHotel(hotelId)
}
