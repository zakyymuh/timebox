package dev.zakyymuh.timebox.core

import java.time.LocalDate
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue

class MotivationTest {
    private val day = LocalDate.of(2026, 11, 8)

    @Test
    fun `the first line is the owner's own`() {
        assertEquals("Plan your day for a greater future.", Motivation.lines.first())
    }

    @Test
    fun `the same day always gets the same line`() {
        assertEquals(Motivation.forDay(day), Motivation.forDay(LocalDate.of(2026, 11, 8)))
    }

    @Test
    fun `the next day gets a different line`() {
        assertNotEquals(Motivation.forDay(day), Motivation.forDay(day.plusDays(1)))
    }

    @Test
    fun `every line comes round within a cycle`() {
        val seen = (0 until Motivation.lines.size).map { Motivation.forDay(day.plusDays(it.toLong())) }.toSet()
        assertEquals(Motivation.lines.toSet(), seen)
    }

    @Test
    fun `lines are short enough for two lines on a phone`() {
        assertTrue(Motivation.lines.all { it.length <= 60 })
    }
}
