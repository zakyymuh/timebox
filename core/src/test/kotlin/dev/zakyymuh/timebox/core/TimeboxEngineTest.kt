package dev.zakyymuh.timebox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class TimeboxEngineTest {

    private val engine = TimeboxEngine()
    private val t0 = Instant.parse("2026-10-27T09:00:00Z")
    private val date = LocalDate.of(2026, 10, 27)

    private fun day(vararg blocks: Block) = DayTimebox(date, "Ship the auth refactor", blocks.toList())

    private fun b(id: Long, pos: Int, mins: Long = 30) =
        Block(id, pos, "block $id", Duration.ofMinutes(mins))

    private fun DayTimebox.block(id: Long) = blocks.single { it.id == id }
    private fun DayTimebox.statuses() = blocks.map { it.status }

    @Test
    fun `starting a pending block makes it running`() {
        val d = engine.start(day(b(1, 0), b(2, 1)), 1, t0)
        assertEquals(BlockStatus.RUNNING, d.block(1).status)
        assertEquals(t0, d.block(1).startedAt)
    }

    @Test
    fun `starting a block finishes whichever block was running`() {
        var d = engine.start(day(b(1, 0), b(2, 1)), 1, t0)
        d = engine.start(d, 2, t0.plusSeconds(600))
        assertEquals(BlockStatus.DONE, d.block(1).status)
        assertEquals(t0.plusSeconds(600), d.block(1).finishedAt)
        assertEquals(BlockStatus.RUNNING, d.block(2).status)
    }

    @Test
    fun `at most one block is ever running`() {
        var d = engine.start(day(b(1, 0), b(2, 1), b(3, 2)), 1, t0)
        d = engine.start(d, 2, t0.plusSeconds(60))
        d = engine.start(d, 3, t0.plusSeconds(120))
        assertEquals(1, d.blocks.count { it.status == BlockStatus.RUNNING })
    }

    @Test
    fun `starting the already-running block changes nothing`() {
        val d = engine.start(day(b(1, 0), b(2, 1)), 1, t0)
        assertEquals(d, engine.start(d, 1, t0.plusSeconds(600)))
    }

    @Test
    fun `finishRunning on a day with nothing running changes nothing`() {
        val d = day(b(1, 0), b(2, 1))
        assertEquals(d, engine.finishRunning(d, t0))
    }

    @Test
    fun `advance finishes the running block and starts the next pending one`() {
        var d = engine.start(day(b(1, 0), b(2, 1)), 1, t0)
        d = engine.advance(d, t0.plusSeconds(900))
        assertEquals(BlockStatus.DONE, d.block(1).status)
        assertEquals(BlockStatus.RUNNING, d.block(2).status)
        assertEquals(t0.plusSeconds(900), d.block(2).startedAt)
    }

    @Test
    fun `advance skips over blocks that are already done`() {
        var d = day(b(1, 0), b(2, 1), b(3, 2))
        d = engine.start(d, 2, t0)                      // run the middle one
        d = engine.advance(d, t0.plusSeconds(60))       // should go to 3, not back to 1
        assertEquals(BlockStatus.RUNNING, d.block(3).status)
        assertEquals(BlockStatus.PENDING, d.block(1).status)
    }

    @Test
    fun `advance on the last block finishes it and starts nothing`() {
        var d = engine.start(day(b(1, 0), b(2, 1)), 2, t0)
        d = engine.advance(d, t0.plusSeconds(60))
        assertNull(engine.running(d))
        assertEquals(BlockStatus.DONE, d.block(2).status)
    }

    @Test
    fun `advance with nothing running starts the first pending block`() {
        val d = engine.advance(day(b(1, 0), b(2, 1)), t0)
        assertEquals(BlockStatus.RUNNING, d.block(1).status)
    }

    @Test
    fun `skip marks a pending block skipped without starting it`() {
        val d = engine.skip(day(b(1, 0), b(2, 1)), 1, t0)
        assertEquals(BlockStatus.SKIPPED, d.block(1).status)
        assertNull(d.block(1).startedAt)
    }

    @Test
    fun `skip on the running block finishes and marks it skipped`() {
        var d = engine.start(day(b(1, 0), b(2, 1)), 1, t0)
        d = engine.skip(d, 1, t0.plusSeconds(300))
        assertEquals(BlockStatus.SKIPPED, d.block(1).status)
        assertEquals(t0.plusSeconds(300), d.block(1).finishedAt)
        assertNull(engine.running(d))
    }

    @Test
    fun `advance does not start a skipped block`() {
        var d = engine.skip(day(b(1, 0), b(2, 1), b(3, 2)), 2, t0)
        d = engine.start(d, 1, t0)
        d = engine.advance(d, t0.plusSeconds(60))
        assertEquals(BlockStatus.RUNNING, d.block(3).status)
        assertEquals(BlockStatus.SKIPPED, d.block(2).status)
    }

    @Test
    fun `restarting a done block clears its finish time`() {
        var d = engine.start(day(b(1, 0), b(2, 1)), 1, t0)
        d = engine.advance(d, t0.plusSeconds(600))      // 1 done, 2 running
        d = engine.start(d, 1, t0.plusSeconds(1200))    // redo 1
        assertEquals(BlockStatus.RUNNING, d.block(1).status)
        assertNull(d.block(1).finishedAt)
        assertEquals(BlockStatus.DONE, d.block(2).status)
    }

    @Test
    fun `running returns null when every block is done`() {
        var d = engine.start(day(b(1, 0), b(2, 1)), 1, t0)
        d = engine.advance(d, t0.plusSeconds(60))
        d = engine.advance(d, t0.plusSeconds(120))
        assertNull(engine.running(d))
        assertTrue(d.statuses().all { it == BlockStatus.DONE })
    }

    @Test
    fun `nextPending ignores done and skipped blocks`() {
        var d = engine.skip(day(b(1, 0), b(2, 1), b(3, 2)), 1, t0)
        d = engine.start(d, 2, t0)
        d = engine.finishRunning(d, t0.plusSeconds(60))
        assertEquals(3L, engine.nextPending(d)?.id)
    }

    @Test
    fun `nextPending is null when nothing is left`() {
        val d = engine.skip(engine.skip(day(b(1, 0), b(2, 1)), 1, t0), 2, t0)
        assertNull(engine.nextPending(d))
    }
}
