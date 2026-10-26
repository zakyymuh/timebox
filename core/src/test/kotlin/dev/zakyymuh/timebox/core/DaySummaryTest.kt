package dev.zakyymuh.timebox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DaySummaryTest {

    private val now = Instant.parse("2026-10-27T09:14:00Z")
    private val date = LocalDate.of(2026, 10, 27)

    private fun item(id: Long, done: Boolean) =
        TodoItem(id, id.toInt(), "item $id", if (done) now else null)

    private fun day(vararg items: TodoItem) = DayList(date, "x", items.toList())

    @Test
    fun `summarise counts done out of total`() {
        val s = DaySummaries.summarise(day(item(1, true), item(2, false), item(3, true)))
        assertEquals(2, s.done)
        assertEquals(3, s.total)
    }

    @Test
    fun `a day with no items is empty`() {
        val s = DaySummaries.summarise(day())
        assertTrue(s.isEmpty)
        assertFalse(s.allDone)
    }

    @Test
    fun `a null day is empty rather than a crash`() {
        // Every morning starts here: no list exists yet for today.
        val s = DaySummaries.summarise(null)
        assertTrue(s.isEmpty)
        assertEquals(0, s.total)
    }

    @Test
    fun `allDone is true only when every item is ticked`() {
        assertTrue(DaySummaries.summarise(day(item(1, true), item(2, true))).allDone)
        assertFalse(DaySummaries.summarise(day(item(1, true), item(2, false))).allDone)
    }

    @Test
    fun `an empty day is not allDone`() {
        // Nothing planned is not the same as everything finished.
        assertFalse(DaySummaries.summarise(day()).allDone)
    }
}
