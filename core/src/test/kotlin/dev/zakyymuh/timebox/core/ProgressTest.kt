package dev.zakyymuh.timebox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class ProgressTest {

    private val t0 = Instant.parse("2026-10-27T09:00:00Z")

    private fun block(
        started: Instant? = null,
        finished: Instant? = null,
        skipped: Boolean = false,
        planned: Duration = Duration.ofMinutes(30),
    ) = Block(1, 0, "Ship the auth refactor", planned, started, finished, skipped)

    @Test
    fun `a block with no timestamps is PENDING`() {
        assertEquals(BlockStatus.PENDING, block().status)
    }

    @Test
    fun `a started but unfinished block is RUNNING`() {
        assertEquals(BlockStatus.RUNNING, block(started = t0).status)
    }

    @Test
    fun `a finished block is DONE`() {
        assertEquals(BlockStatus.DONE, block(started = t0, finished = t0.plusSeconds(60)).status)
    }

    @Test
    fun `a skipped block is SKIPPED even when it was started`() {
        assertEquals(BlockStatus.SKIPPED, block(started = t0, skipped = true).status)
    }

    @Test
    fun `progress is null for a block that never started`() {
        assertNull(Progress.of(block(), t0))
    }

    @Test
    fun `elapsed counts from startedAt to now while running`() {
        val p = Progress.of(block(started = t0), t0.plusSeconds(600))!!
        assertEquals(Duration.ofMinutes(10), p.elapsed)
    }

    @Test
    fun `elapsed counts from startedAt to finishedAt once done`() {
        // Review Focus 2: elapsed is derived from stored instants, never a live timer,
        // so a finished block must not keep growing as the clock moves on.
        val p = Progress.of(block(started = t0, finished = t0.plusSeconds(300)), t0.plusSeconds(9999))!!
        assertEquals(Duration.ofMinutes(5), p.elapsed)
    }

    @Test
    fun `remaining is floored at zero rather than going negative`() {
        val p = Progress.of(block(started = t0), t0.plusSeconds(3600))!!
        assertEquals(Duration.ZERO, p.remaining)
    }

    @Test
    fun `remaining is the unused part of the planned length`() {
        val p = Progress.of(block(started = t0), t0.plusSeconds(600))!!
        assertEquals(Duration.ofMinutes(20), p.remaining)
    }

    @Test
    fun `overrunBy is zero before the planned length is reached`() {
        val p = Progress.of(block(started = t0), t0.plusSeconds(600))!!
        assertEquals(Duration.ZERO, p.overrunBy)
        assertFalse(p.isOverrun)
    }

    @Test
    fun `overrunBy is the excess once past the planned length`() {
        val p = Progress.of(block(started = t0), t0.plusSeconds(2520))!!
        assertEquals(Duration.ofMinutes(12), p.overrunBy)
        assertTrue(p.isOverrun)
    }

    @Test
    fun `a block running across midnight reports the full elapsed time`() {
        // Review Focus 3: nothing clips a block at the day boundary.
        val late = Instant.parse("2026-10-27T16:40:00Z")   // 23:40 Jakarta
        val p = Progress.of(block(started = late, planned = Duration.ofMinutes(20)), late.plusSeconds(1800))!!
        assertEquals(Duration.ofMinutes(30), p.elapsed)
        assertEquals(Duration.ofMinutes(10), p.overrunBy)
    }
}
