package com.pms.pms.booking.application

import com.pms.pms.api.v1.reservations.dto.CreateHoldRequest
import com.pms.pms.api.v1.reservations.dto.CreateHoldResponse
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class ReservationHoldService {
    suspend fun createHold(_idemKey: String, _req: CreateHoldRequest): CreateHoldResponse {
        return CreateHoldResponse(
            holdId = UUID.randomUUID().toString(),
            roomId = UUID.randomUUID().toString(),
            status = "HOLD CREATED",
            expiresAt = OffsetDateTime.now().plusMinutes(15).toString()
        )
    }
}

class NoAvailabilityException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)