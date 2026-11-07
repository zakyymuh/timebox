package dev.zakyymuh.timebox

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** A clock a test can move, e.g. past midnight. */
class TestClock(var now: Instant, private val zone: ZoneId) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = TestClock(now, zone)
    override fun instant(): Instant = now
}
