package dev.zakyymuh.timebox.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class DaySummaryTest {

    private val t0 = Instant.parse("2026-10-27T09:00:00Z")
    private val date = LocalDate.of(2026, 10, 27)

    private fun b(id: Long, pos: Int, mins: Long) =
        Block(id, pos, "block $id", Duration.ofMinutes(mins))

    private fun day(vararg blocks: Block) = DayTimebox(date, "x", blocks.toList())

    @Test
    fun `summarise totals the planned durations of every block`() {
        val s = DaySummaries.summarise(day(b(1, 0, 30), b(2, 1, 90)), t0)
        assertEquals(Duration.ofMinutes(120), s.plannedTotal)
    }

    @Test
    fun `summarise totals actual time from done blocks only`() {
        val done = b(1, 0, 30).copy(startedAt = t0, finishedAt = t0.plusSeconds(600))
        val s = DaySummaries.summarise(day(done, b(2, 1, 90)), t0.plusSeconds(600))
        assertEquals(Duration.ofMinutes(10), s.actualTotal)
    }

    @Test
    fun `summarise includes the running block's elapsed time in actualTotal`() {
        val done = b(1, 0, 30).copy(startedAt = t0, finishedAt = t0.plusSeconds(600))
        val running = b(2, 1, 90).copy(startedAt = t0.plusSeconds(600))
        val s = DaySummaries.summarise(day(done, running), t0.plusSeconds(1200))
        assertEquals(Duration.ofMinutes(20), s.actualTotal)
    }

    @Test
    fun `summarise counts done, skipped and total`() {
        val done = b(1, 0, 30).copy(startedAt = t0, finishedAt = t0.plusSeconds(60))
        val skipped = b(2, 1, 30).copy(skipped = true)
        val s = DaySummaries.summarise(day(done, skipped, b(3, 2, 30)), t0)
        assertEquals(1, s.done)
        assertEquals(1, s.skipped)
        assertEquals(3, s.total)
    }

    @Test
    fun `summarise of an empty day is all zeroes`() {
        val s = DaySummaries.summarise(DayTimebox(date, null, emptyList()), t0)
        assertEquals(Duration.ZERO, s.plannedTotal)
        assertEquals(Duration.ZERO, s.actualTotal)
        assertEquals(0, s.total)
    }

    @Test
    fun `a skipped block contributes no actual time even if it ran`() {
        val skipped = b(1, 0, 30).copy(startedAt = t0, finishedAt = t0.plusSeconds(600), skipped = true)
        val s = DaySummaries.summarise(day(skipped), t0.plusSeconds(600))
        assertEquals(Duration.ZERO, s.actualTotal)
    }
}
