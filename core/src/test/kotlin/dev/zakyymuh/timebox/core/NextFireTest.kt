package dev.zakyymuh.timebox.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NextFireTest {

    private val jkt = ZoneId.of("Asia/Jakarta")
    private val sixThirty = LocalTime.of(6, 30)

    @Test
    fun `fires later today when the time has not passed`() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(5, 0), jkt)
        val next = NextFire.compute(sixThirty, now)
        assertEquals(LocalDate.of(2026, 10, 26), next.toLocalDate())
        assertEquals(sixThirty, next.toLocalTime())
    }

    @Test
    fun `fires tomorrow when the time has already passed`() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 10, 26), LocalTime.of(9, 14), jkt)
        assertEquals(LocalDate.of(2026, 10, 27), NextFire.compute(sixThirty, now).toLocalDate())
    }

    @Test
    fun `fires tomorrow when now is exactly the target time`() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 10, 26), sixThirty, jkt)
        assertEquals(LocalDate.of(2026, 10, 27), NextFire.compute(sixThirty, now).toLocalDate())
    }

    @Test
    fun `crosses a month boundary correctly`() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 10, 31), LocalTime.of(23, 0), jkt)
        val next = NextFire.compute(sixThirty, now)
        assertEquals(LocalDate.of(2026, 11, 1), next.toLocalDate())
        assertEquals(sixThirty, next.toLocalTime())
    }

    @Test
    fun `keeps the wall-clock time across a DST transition`() {
        // London moves to BST in the small hours of 2027-03-28.
        val london = ZoneId.of("Europe/London")
        val now = ZonedDateTime.of(LocalDate.of(2027, 3, 27), LocalTime.of(22, 0), london)
        val next = NextFire.compute(sixThirty, now)
        assertEquals(LocalDate.of(2027, 3, 28), next.toLocalDate())
        assertEquals(sixThirty, next.toLocalTime())
    }
}
