package com.pms.pms.shared.errors

class IdempotencyKeyConflictException(
    message: String = "Idempotency key conflict: payload differs from original request."
) : RuntimeException(message)
