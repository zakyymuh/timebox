package dev.zakyymuh.timebox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class BlocksTest {

    private val t0 = Instant.parse("2026-10-27T09:00:00Z")
    private val yesterday = LocalDate.of(2026, 10, 26)
    private val today = LocalDate.of(2026, 10, 27)

    private fun b(id: Long, pos: Int, title: String = "block $id", mins: Long = 30) =
        Block(id, pos, title, Duration.ofMinutes(mins))

    @Test
    fun `reorder moves a block and renumbers positions contiguously from zero`() {
        val out = Blocks.reorder(listOf(b(1, 0), b(2, 1), b(3, 2)), fromIndex = 0, toIndex = 2)
        assertEquals(listOf(2L, 3L, 1L), out.map { it.id })
        assertEquals(listOf(0, 1, 2), out.map { it.position })
    }

    @Test
    fun `reorder to the same index changes nothing`() {
        val input = listOf(b(1, 0), b(2, 1))
        assertEquals(input, Blocks.reorder(input, 1, 1))
    }

    @Test
    fun `renumber closes gaps left by a deletion`() {
        val out = Blocks.renumber(listOf(b(1, 0), b(3, 7), b(4, 9)))
        assertEquals(listOf(0, 1, 2), out.map { it.position })
        assertEquals(listOf(1L, 3L, 4L), out.map { it.id })
    }

    @Test
    fun `templateFrom clears every startedAt, finishedAt and skipped flag`() {
        // Review Focus 5: a plan copied from a recent day must arrive empty of history,
        // or the new day shows as finished before it began.
        val done = b(1, 0).copy(startedAt = t0, finishedAt = t0.plusSeconds(600))
        val skipped = b(2, 1).copy(skipped = true, finishedAt = t0)
        val source = DayTimebox(yesterday, "yesterday's vision", listOf(done, skipped))

        val out = Blocks.templateFrom(source, today)

        assertTrue(out.blocks.all { it.startedAt == null })
        assertTrue(out.blocks.all { it.finishedAt == null })
        assertTrue(out.blocks.none { it.skipped })
        assertTrue(out.blocks.all { it.status == BlockStatus.PENDING })
    }

    @Test
    fun `templateFrom keeps titles, planned lengths and order`() {
        val source = DayTimebox(
            yesterday, "x",
            listOf(b(1, 0, "Workout", 60), b(2, 1, "Thesis", 120)),
        )
        val out = Blocks.templateFrom(source, today)
        assertEquals(listOf("Workout", "Thesis"), out.blocks.map { it.title })
        assertEquals(listOf(Duration.ofMinutes(60), Duration.ofMinutes(120)), out.blocks.map { it.planned })
        assertEquals(listOf(0, 1), out.blocks.map { it.position })
    }

    @Test
    fun `templateFrom sets the new date and drops the old intention`() {
        val source = DayTimebox(yesterday, "yesterday's vision", listOf(b(1, 0)))
        val out = Blocks.templateFrom(source, today)
        assertEquals(today, out.date)
        assertNull(out.intention)
    }

    @Test
    fun `templateFrom clears ids so the copies are new rows`() {
        val source = DayTimebox(yesterday, "x", listOf(b(7, 0), b(9, 1)))
        assertTrue(Blocks.templateFrom(source, today).blocks.all { it.id == 0L })
    }
}
