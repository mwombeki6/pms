package com.pms.pms.booking.application

import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "booking.holds")
data class BookingHoldProperties(
    @field:Min(1)
    val ttlMinutes: Long = 15,
    @field:Min(1000)
    val expireIntervalMs: Long = 60000
)
