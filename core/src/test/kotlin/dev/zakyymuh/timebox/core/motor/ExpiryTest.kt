package dev.zakyymuh.timebox.core.motor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExpiryTest {
    private val today = LocalDate.of(2026, 11, 9)

    private fun expiry(date: LocalDate, name: String = "STNK tax", months: Int = 12, id: Long = 1) =
        Expiry(id, vehicleId = 1, name = name, date = date, renewMonths = months)

    @Test
    fun `the line says when, and how long from now`() {
        assertEquals("expires 12 Mar 2027 · in 4 months", Expiries.line(LocalDate.of(2027, 3, 12), today))
        assertEquals("expires 9 Nov 2026 · today", Expiries.line(today, today))
        assertEquals("expires 10 Nov 2026 · tomorrow", Expiries.line(today.plusDays(1), today))
        assertEquals("expires 16 Nov 2026 · in 7 days", Expiries.line(today.plusDays(7), today))
        assertEquals("expires 9 Dec 2026 · in 30 days", Expiries.line(today.plusDays(30), today))
        assertEquals("expires 9 Nov 2031 · in 5 years", Expiries.line(today.plusYears(5), today))
        assertEquals("expires 9 Jan 2028 · in 1 year", Expiries.line(LocalDate.of(2028, 1, 9), today))
    }

    @Test
    fun `once it has passed it says how long ago`() {
        assertEquals("expired 8 Nov 2026 · yesterday", Expiries.line(today.minusDays(1), today))
        assertEquals("expired 6 Nov 2026 · 3 days ago", Expiries.line(today.minusDays(3), today))
        assertEquals("expired 9 Jul 2026 · 4 months ago", Expiries.line(LocalDate.of(2026, 7, 9), today))
    }

    @Test
    fun `overdue starts the day after the date, soon is the 30 days before`() {
        assertFalse(Expiries.isOverdue(today, today))
        assertTrue(Expiries.isOverdue(today.minusDays(1), today))
        assertTrue(Expiries.isSoon(today, today))
        assertTrue(Expiries.isSoon(today.plusDays(30), today))
        assertFalse(Expiries.isSoon(today.plusDays(31), today))
        assertFalse(Expiries.isSoon(today.minusDays(1), today))
    }

    @Test
    fun `renewing moves the date on by its period`() {
        assertEquals(LocalDate.of(2028, 3, 12), Expiries.renewed(expiry(LocalDate.of(2027, 3, 12))))
        assertEquals(LocalDate.of(2031, 3, 12), Expiries.renewed(expiry(LocalDate.of(2026, 3, 12), "SIM C", 60)))
        // A leap day lands on the last day of February.
        assertEquals(LocalDate.of(2029, 2, 28), Expiries.renewed(expiry(LocalDate.of(2028, 2, 29))))
    }

    @Test
    fun `soonest first, overdue on top`() {
        val list = listOf(
            expiry(today.plusDays(40), "Insurance", id = 1),
            expiry(today.minusDays(2), "KIR", id = 2),
            expiry(today.plusDays(3), "SIM C", id = 3),
        )
        assertEquals(listOf("KIR", "SIM C", "Insurance"), Expiries.soonestFirst(list).map { it.name })
    }

    @Test
    fun `a form needs a name and a sensible renewal`() {
        assertFalse(ExpiryForm(name = " ", date = today).isValid())
        assertTrue(ExpiryForm(name = "SIM C", date = today, renewMonths = 60).isValid())
        assertFalse(ExpiryForm(name = "SIM C", date = today, renewMonths = 0).isValid())
    }

    @Test
    fun `renewal periods read naturally`() {
        assertEquals("1 year", Expiries.period(12))
        assertEquals("5 years", Expiries.period(60))
        assertEquals("6 months", Expiries.period(6))
    }
}
