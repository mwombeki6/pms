package com.pms.pms.booking.application

import com.pms.pms.booking.domain.ReservationHoldStatus
import java.time.Clock
import java.time.OffsetDateTime
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ReservationHoldMaintenance(
    private val databaseClient: DatabaseClient,
    private val clock: Clock
) {
    @Scheduled(fixedDelayString = "\${booking.holds.expire-interval-ms:60000}")
    fun expireHoldsOnSchedule() {
        runBlocking {
            expireHolds(OffsetDateTime.now(clock))
        }
    }

    suspend fun expireHolds(now: OffsetDateTime) {
        val sql = """
            update reservation_holds
            set status = :expiredStatus
            where status = :activeStatus
              and expires_at <= :now
        """.trimIndent()

        databaseClient.sql(sql)
            .bind("expiredStatus", ReservationHoldStatus.EXPIRED.name)
            .bind("activeStatus", ReservationHoldStatus.HOLD_CREATED.name)
            .bind("now", now)
            .fetch()
            .rowsUpdated()
            .awaitSingleOrNull()
    }
}
