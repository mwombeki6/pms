CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID NOT NULL,
    room_type TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE reservation_holds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id UUID NOT NULL,
    room_id UUID NOT NULL REFERENCES rooms(id),
    guest_name TEXT NOT NULL,
    guest_phone TEXT NOT NULL,
    stay_range TSTZRANGE NOT NULL,
    status TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE reservation_holds
    ADD CONSTRAINT no_overlapping_holds
    EXCLUDE USING gist (
        room_id WITH =,
        stay_range WITH &&
    )
    WHERE (status IN ('HOLD_CREATED', 'CONFIRMED'));

CREATE TABLE idempotency_keys (
    scope TEXT NOT NULL,
    idem_key TEXT NOT NULL,
    request_hash TEXT NOT NULL,
    response_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (scope, idem_key)
)
