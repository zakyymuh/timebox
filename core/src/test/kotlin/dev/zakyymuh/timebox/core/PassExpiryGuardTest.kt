package dev.zakyymuh.timebox.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class PassExpiryGuardTest {

    private val now = Instant.parse("2026-10-26T09:14:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private class FakeScheduler : DelayScheduler {
        val log = mutableListOf<String>()
        val pending = mutableMapOf<String, Pair<Long, () -> Unit>>()

        override fun postDelayed(key: String, delayMillis: Long, action: () -> Unit) {
            log += "post:$key:$delayMillis"
            pending[key] = delayMillis to action
        }

        override fun cancel(key: String) {
            log += "cancel:$key"
            pending.remove(key)
        }

        fun fire(key: String) = pending.getValue(key).second.invoke()
    }

    private fun pass(pkg: String, expiresAt: Instant) =
        Pass(1, pkg, now, expiresAt, "need the DM from Rina about the venue")

    @Test
    fun `arm schedules a re-check at the moment the pass expires`() {
        val s = FakeScheduler()
        PassExpiryGuard(s, clock).arm(pass("com.instagram.android", now.plusSeconds(300))) {}
        assertEquals(300_000L, s.pending.getValue("com.instagram.android").first)
    }

    @Test
    fun `arm keys the schedule by package so two passes do not collide`() {
        val s = FakeScheduler()
        val guard = PassExpiryGuard(s, clock)
        guard.arm(pass("com.instagram.android", now.plusSeconds(300))) {}
        guard.arm(pass("com.twitter.android", now.plusSeconds(60))) {}
        assertEquals(setOf("com.instagram.android", "com.twitter.android"), s.pending.keys)
        assertEquals(300_000L, s.pending.getValue("com.instagram.android").first)
        assertEquals(60_000L, s.pending.getValue("com.twitter.android").first)
    }

    @Test
    fun `an already-expired pass is re-checked immediately with zero delay`() {
        val s = FakeScheduler()
        PassExpiryGuard(s, clock).arm(pass("com.instagram.android", now.minusSeconds(90))) {}
        assertEquals(0L, s.pending.getValue("com.instagram.android").first)
    }

    @Test
    fun `re-arming the same package replaces the earlier schedule`() {
        val s = FakeScheduler()
        val guard = PassExpiryGuard(s, clock)
        guard.arm(pass("com.instagram.android", now.plusSeconds(60))) {}
        guard.arm(pass("com.instagram.android", now.plusSeconds(300))) {}
        assertEquals(
            listOf(
                "cancel:com.instagram.android",
                "post:com.instagram.android:60000",
                "cancel:com.instagram.android",
                "post:com.instagram.android:300000",
            ),
            s.log,
        )
        assertEquals(1, s.pending.size)
        assertEquals(300_000L, s.pending.getValue("com.instagram.android").first)
    }

    @Test
    fun `the expiry callback receives the package name`() {
        val s = FakeScheduler()
        var fired: String? = null
        PassExpiryGuard(s, clock).arm(pass("com.instagram.android", now.plusSeconds(300))) { fired = it }
        s.fire("com.instagram.android")
        assertEquals("com.instagram.android", fired)
    }
}
