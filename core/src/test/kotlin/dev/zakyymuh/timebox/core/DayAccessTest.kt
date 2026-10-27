package dev.zakyymuh.timebox.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayAccessTest {

    private val today = LocalDate.of(2026, 10, 27)

    @Test
    fun `today is editable`() {
        assertTrue(DayAccess.isEditable(today, today))
    }

    @Test
    fun `yesterday is read-only`() {
        assertFalse(DayAccess.isEditable(today.minusDays(1), today))
    }

    @Test
    fun `a day last week is read-only`() {
        assertFalse(DayAccess.isEditable(today.minusDays(7), today))
    }

    @Test
    fun `a future day is not editable either`() {
        // Not reachable by stepping through the record, but if a stale date ever got
        // there it must not become a second editable day.
        assertFalse(DayAccess.isEditable(today.plusDays(1), today))
        assertTrue(DayAccess.isFuture(today.plusDays(1), today))
    }

    @Test
    fun `today is not in the future`() {
        assertFalse(DayAccess.isFuture(today, today))
    }
}
