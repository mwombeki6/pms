package com.pms.pms.booking.application

import com.pms.pms.api.v1.reservations.dto.CreateHoldRequest
import com.pms.pms.api.v1.reservations.dto.CreateHoldResponse

class ReservationHoldService {
    suspend fun  createHold(idemKey: String, req: CreateHoldRequest): CreateHoldResponse
}