package com.pms.pms.room.infrastructure

import com.pms.pms.room.domain.Room
import java.util.UUID
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository

@Repository
class RoomRepository(
    private val databaseClient: DatabaseClient
) {
    suspend fun findByHotelId(hotelId: UUID): List<Room> {
        val sql = """
            select id, hotel_id, room_type, is_active
            from rooms
            where hotel_id = :hotelId
            order by id
        """.trimIndent()

        return databaseClient.sql(sql)
            .bind("hotelId", hotelId)
            .map { row, _ ->
                Room(
                    id = row.get("id", UUID::class.java)!!,
                    hotelId = row.get("hotel_id", UUID::class.java)!!,
                    roomType = row.get("room_type", String::class.java)!!,
                    isActive = row.get("is_active", Boolean::class.javaObjectType) ?: false
                )
            }
            .all()
            .collectList()
            .awaitSingle()
    }
}
