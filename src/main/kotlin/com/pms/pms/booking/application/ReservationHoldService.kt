package com.pms.pms.booking.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.pms.pms.api.v1.reservations.dto.CreateHoldRequest
import com.pms.pms.api.v1.reservations.dto.CreateHoldResponse
import com.pms.pms.shared.errors.IdempotencyKeyConflictException
import com.pms.pms.shared.errors.InvalidRequestException
import com.pms.pms.shared.errors.NoAvailabilityException
import io.r2dbc.postgresql.codec.Json
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservationHoldService(
    private val databaseClient: DatabaseClient,
    private val objectMapper: ObjectMapper,
    private val clock: Clock = Clock.systemUTC()
) {
    companion object {
        private const val IDEM_SCOPE = "reservation-hold:create"
        private const val HOLD_MINUTES: Long = 15
        private val ACTIVE_STATUSES = listOf("HOLD_CREATED", "CONFIRMED")
    }

    @Transactional
    suspend fun createHold(idemKey: String, req: CreateHoldRequest): CreateHoldResponse {
        val requestHash = hashRequest(req)
        val existing = findIdempotencyRecord(idemKey)
        if (existing != null) {
            if (existing.requestHash != requestHash) {
                throw IdempotencyKeyConflictException()
            }
            return existing.response
        }

        val hotelId = parseUuid(req.hotelId, "hotelId")
        val roomId = findAvailableRoom(hotelId, req.checkIn, req.checkOut)
            ?: throw NoAvailabilityException("No availability for requested stay range.")

        val expiresAt = OffsetDateTime.now(clock).plusMinutes(HOLD_MINUTES)
        val holdId = insertHold(hotelId, roomId, req, expiresAt)

        val response = CreateHoldResponse(
            holdId = holdId.toString(),
            roomId = roomId.toString(),
            status = "HOLD_CREATED",
            expiresAt = expiresAt.toString()
        )

        try {
            insertIdempotencyRecord(idemKey, requestHash, response)
        } catch (ex: DataIntegrityViolationException) {
            val afterConflict = findIdempotencyRecord(idemKey)
            if (afterConflict != null && afterConflict.requestHash == requestHash) {
                return afterConflict.response
            }
            throw IdempotencyKeyConflictException()
        }

        return response
    }

    private suspend fun findAvailableRoom(
        hotelId: UUID,
        checkIn: OffsetDateTime,
        checkOut: OffsetDateTime
    ): UUID? {
        val sql = """
            select r.id
            from rooms r
            where r.hotel_id = :hotelId
              and r.is_active = true
              and not exists (
                select 1
                from reservation_holds h
                where h.room_id = r.id
                  and h.status in (${ACTIVE_STATUSES.joinToString(",") { "'$it'" }})
                  and h.stay_range && tstzrange(:checkIn, :checkOut, '[)')
              )
            limit 1
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("hotelId", hotelId)
            .bind("checkIn", checkIn)
            .bind("checkOut", checkOut)
            .map { row, _ -> row.get("id", UUID::class.java) }
            .one()
            .awaitSingleOrNull()
    }

    private suspend fun insertHold(
        hotelId: UUID,
        roomId: UUID,
        req: CreateHoldRequest,
        expiresAt: OffsetDateTime
    ): UUID {
        val sql = """
            insert into reservation_holds (
                hotel_id,
                room_id,
                guest_name,
                guest_phone,
                stay_range,
                status,
                expires_at
            )
            values (
                :hotelId,
                :roomId,
                :guestName,
                :guestPhone,
                tstzrange(:checkIn, :checkOut, '[)'),
                :status,
                :expiresAt
            )
            returning id
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("hotelId", hotelId)
            .bind("roomId", roomId)
            .bind("guestName", req.guestName)
            .bind("guestPhone", req.guestPhone)
            .bind("checkIn", req.checkIn)
            .bind("checkOut", req.checkOut)
            .bind("status", "HOLD_CREATED")
            .bind("expiresAt", expiresAt)
            .map { row, _ -> row.get("id", UUID::class.java)!! }
            .one()
            .awaitSingle()
    }

    private suspend fun findIdempotencyRecord(idemKey: String): IdempotencyRecord? {
        val sql = """
            select request_hash, response_json
            from idempotency_keys
            where scope = :scope and idem_key = :idemKey
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("scope", IDEM_SCOPE)
            .bind("idemKey", idemKey)
            .map { row, _ ->
                val hash = row.get("request_hash", String::class.java) ?: ""
                val responseJson = readJson(row.get("response_json"))
                val response = objectMapper.readValue(responseJson, CreateHoldResponse::class.java)
                IdempotencyRecord(hash, response)
            }
            .one()
            .awaitSingleOrNull()
    }

    private suspend fun insertIdempotencyRecord(
        idemKey: String,
        requestHash: String,
        response: CreateHoldResponse
    ) {
        val sql = """
            insert into idempotency_keys (scope, idem_key, request_hash, response_json)
            values (:scope, :idemKey, :requestHash, :responseJson)
        """.trimIndent()

        val responseJson = objectMapper.writeValueAsString(response)

        databaseClient.sql(sql)
            .bind("scope", IDEM_SCOPE)
            .bind("idemKey", idemKey)
            .bind("requestHash", requestHash)
            .bind("responseJson", responseJson)
            .then()
            .awaitSingleOrNull()
    }

    private fun hashRequest(req: CreateHoldRequest): String {
        val payload = listOf(
            req.hotelId,
            req.guestName,
            req.guestPhone,
            req.checkIn.toString(),
            req.checkOut.toString()
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun parseUuid(value: String, field: String): UUID =
        try {
            UUID.fromString(value)
        } catch (ex: IllegalArgumentException) {
            throw InvalidRequestException("$field must be a valid UUID")
        }

    private fun readJson(value: Any?): String =
        when (value) {
            is Json -> value.asString()
            is String -> value
            null -> "{}"
            else -> objectMapper.writeValueAsString(value)
        }

    private data class IdempotencyRecord(
        val requestHash: String,
        val response: CreateHoldResponse
    )
}
