ALTER TABLE reservation_holds
    ADD CONSTRAINT reservation_holds_status_check
    CHECK (status IN ('HOLD_CREATED', 'CONFIRMED', 'CANCELLED', 'EXPIRED'));

CREATE INDEX IF NOT EXISTS idx_rooms_hotel_id
    ON rooms (hotel_id);

CREATE INDEX IF NOT EXISTS idx_reservation_holds_room_id
    ON reservation_holds (room_id);

CREATE INDEX IF NOT EXISTS idx_reservation_holds_status_expires_at
    ON reservation_holds (status, expires_at);
