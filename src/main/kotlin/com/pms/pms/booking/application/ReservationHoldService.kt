package com.pms.pms.booking.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.pms.pms.booking.api.dto.CreateHoldRequest
import com.pms.pms.booking.api.dto.CreateHoldResponse
import com.pms.pms.booking.domain.ReservationHoldStatus
import com.pms.pms.shared.errors.IdempotencyKeyConflictException
import com.pms.pms.shared.errors.NoAvailabilityException
import com.pms.pms.shared.utils.parseUuidOrThrow
import io.r2dbc.postgresql.codec.Json
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservationHoldService(
    private val databaseClient: DatabaseClient,
    private val objectMapper: ObjectMapper,
    private val clock: Clock = Clock.systemUTC(),
    private val holdProperties: BookingHoldProperties,
    private val holdMaintenance: ReservationHoldMaintenance
) {
    companion object {
        private const val IDEM_SCOPE = "reservation-hold:create"
    }

    @Transactional
    suspend fun createHold(idemKey: String, req: CreateHoldRequest): CreateHoldResponse {
        lockIdempotencyKey(idemKey)
        val requestHash = hashRequest(req)
        val existing = findIdempotencyRecord(idemKey)
        if (existing != null) {
            if (existing.requestHash != requestHash) {
                throw IdempotencyKeyConflictException()
            }
            return existing.response
        }

        val hotelId = parseUuidOrThrow(req.hotelId, "hotelId")
        val now = OffsetDateTime.now(clock)
        holdMaintenance.expireHolds(now)

        val expiresAt = now.plusMinutes(holdProperties.ttlMinutes)
        val holdResult = try {
            insertHoldIfAvailable(hotelId, req, expiresAt, now)
        } catch (_ex: DataIntegrityViolationException) {
            null
        } ?: throw NoAvailabilityException("No availability for requested stay range.")

        val response = CreateHoldResponse(
            holdId = holdResult.holdId.toString(),
            roomId = holdResult.roomId.toString(),
            status = ReservationHoldStatus.HOLD_CREATED.name,
            expiresAt = expiresAt.toString()
        )

        try {
            insertIdempotencyRecord(idemKey, requestHash, response)
        } catch (_ex: DataIntegrityViolationException) {
            val afterConflict = findIdempotencyRecord(idemKey)
            if (afterConflict != null && afterConflict.requestHash == requestHash) {
                return afterConflict.response
            }
            throw IdempotencyKeyConflictException()
        }

        return response
    }

    private suspend fun lockIdempotencyKey(idemKey: String) {
        val sql = """
            select pg_advisory_xact_lock(hashtextextended(:lockKey, 0))
        """.trimIndent()

        databaseClient.sql(sql)
            .bind("lockKey", "$IDEM_SCOPE:$idemKey")
            .then()
            .awaitSingleOrNull()
    }

    private suspend fun insertHoldIfAvailable(
        hotelId: UUID,
        req: CreateHoldRequest,
        expiresAt: OffsetDateTime,
        now: OffsetDateTime
    ): HoldInsertResult? {
        val sql = """
            with candidate as (
                select r.id
                from rooms r
                where r.hotel_id = :hotelId
                  and r.is_active = true
                  and not exists (
                    select 1
                    from reservation_holds h
                    where h.room_id = r.id
                      and (
                        (h.status = :holdStatus and h.expires_at > :now)
                        or h.status = :confirmedStatus
                      )
                      and h.stay_range && tstzrange(:checkIn, :checkOut, '[)')
                  )
                order by r.id
                for update skip locked
                limit 1
            )
            insert into reservation_holds (
                hotel_id,
                room_id,
                guest_name,
                guest_phone,
                stay_range,
                status,
                expires_at
            )
            select
                :hotelId,
                candidate.id,
                :guestName,
                :guestPhone,
                tstzrange(:checkIn, :checkOut, '[)'),
                :status,
                :expiresAt
            from candidate
            returning id, room_id
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("hotelId", hotelId)
            .bind("guestName", req.guestName)
            .bind("guestPhone", req.guestPhone)
            .bind("checkIn", req.checkIn)
            .bind("checkOut", req.checkOut)
            .bind("status", ReservationHoldStatus.HOLD_CREATED.name)
            .bind("expiresAt", expiresAt)
            .bind("now", now)
            .bind("holdStatus", ReservationHoldStatus.HOLD_CREATED.name)
            .bind("confirmedStatus", ReservationHoldStatus.CONFIRMED.name)
            .map { row, _ ->
                HoldInsertResult(
                    holdId = row.get("id", UUID::class.java)!!,
                    roomId = row.get("room_id", UUID::class.java)!!
                )
            }
            .one()
            .awaitSingleOrNull()
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

    private data class HoldInsertResult(
        val holdId: UUID,
        val roomId: UUID
    )
}
