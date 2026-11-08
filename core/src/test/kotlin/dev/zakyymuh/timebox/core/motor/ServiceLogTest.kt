package dev.zakyymuh.timebox.core.motor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ServiceLogTest {
    private val today = LocalDate.of(2026, 11, 8)
    private fun record(id: Long, date: LocalDate, place: String = "AHASS", price: Long? = 150_000, km: Int? = null) =
        ServiceRecord(id = id, date = date, place = place, price = price, work = "", odometerKm = km)

    @Test fun `nothing logged invites the first entry`() =
        assertEquals("No service logged yet", ServiceLog.summary(emptyList(), today))

    @Test fun `the summary names the latest service, where and how long ago`() {
        val records = listOf(record(1, LocalDate.of(2026, 8, 1), "Bengkel Jaya"), record(2, LocalDate.of(2026, 10, 12)))
        assertEquals("Last: 12 Oct at AHASS · 27 days ago", ServiceLog.summary(records, today))
    }

    @Test fun `today and yesterday read naturally`() {
        assertEquals("Last: today at AHASS", ServiceLog.summary(listOf(record(1, today)), today))
        assertEquals("Last: yesterday at AHASS", ServiceLog.summary(listOf(record(1, today.minusDays(1))), today))
    }

    @Test fun `history is newest first`() {
        val records = listOf(record(1, LocalDate.of(2026, 3, 1)), record(2, LocalDate.of(2026, 10, 1)), record(3, LocalDate.of(2026, 6, 1)))
        assertEquals(listOf(2L, 3L, 1L), ServiceLog.newestFirst(records).map { it.id })
    }

    @Test fun `spent this year adds up only this year's prices`() {
        val records = listOf(
            record(1, LocalDate.of(2025, 12, 30), price = 500_000),
            record(2, LocalDate.of(2026, 2, 1), price = 150_000),
            record(3, LocalDate.of(2026, 9, 1), price = 275_000),
            record(4, LocalDate.of(2026, 10, 1), price = null),
        )
        assertEquals(425_000L, ServiceLog.spentIn(records, 2026))
    }

    @Test fun `distance since the last service needs two odometer readings`() {
        assertEquals(null, ServiceLog.kmSincePrevious(listOf(record(1, today, km = 12_000))))
        val records = listOf(record(1, LocalDate.of(2026, 6, 1), km = 9_500), record(2, today, km = 12_000))
        assertEquals(2_500, ServiceLog.kmSincePrevious(records))
    }

    @Test fun `rupiah uses dots`() = assertEquals("Rp 1.250.000", Money.rupiah(1_250_000))

    @Test fun `a form needs a place and a date not in the future`() {
        assertTrue(ServiceForm(date = today, place = "AHASS").isValid(today))
        assertFalse(ServiceForm(date = today, place = " ").isValid(today))
        assertFalse(ServiceForm(date = today.plusDays(1), place = "AHASS").isValid(today))
    }

    @Test fun `negative price or odometer is invalid`() {
        assertFalse(ServiceForm(date = today, place = "AHASS", price = -1).isValid(today))
        assertFalse(ServiceForm(date = today, place = "AHASS", odometerKm = -5).isValid(today))
    }
}
