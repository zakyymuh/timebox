package dev.zakyymuh.timebox.core.lastdone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

class LastDoneTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val today = LocalDate.of(2026, 11, 9)

    /** Noon in Jakarta, [daysAgo] days before today. */
    private fun at(daysAgo: Long, hour: Int = 12): Instant =
        today.minusDays(daysAgo).atTime(hour, 0).atZone(zone).toInstant()

    @Test
    fun `ago reads like speech`() {
        assertEquals("today", LastDone.ago(today, today))
        assertEquals("yesterday", LastDone.ago(today.minusDays(1), today))
        assertEquals("7 days ago", LastDone.ago(today.minusDays(7), today))
        assertEquals("30 days ago", LastDone.ago(today.minusDays(30), today))
        assertEquals("about 2 months ago", LastDone.ago(today.minusDays(65), today))
        assertEquals("over a year ago", LastDone.ago(today.minusDays(400), today))
    }

    @Test
    fun `ago is counted in calendar days, not 24-hour blocks`() {
        // 23:50 last night is yesterday, even at 00:10 this morning.
        val lateLastNight = at(1, hour = 23)
        val summary = LastDone.summarise(listOf(Mark("laundry", lateLastNight)), zone)
        assertEquals(1, summary.single().daysSince(today))
    }

    @Test
    fun `marks group by name, ignoring case and spaces`() {
        val summary = LastDone.summarise(
            listOf(
                Mark("Laundry", at(14)),
                Mark(" laundry ", at(7)),
                Mark("haircut", at(30)),
            ),
            zone,
        )
        val laundry = summary.first { it.key == "laundry" }
        assertEquals(2, laundry.count)
        assertEquals(today.minusDays(7), laundry.lastDay)
        assertEquals(today.minusDays(14), laundry.firstDay)
        // Shown with the spelling used most recently.
        assertEquals("laundry", laundry.name)
    }

    @Test
    fun `longest ago comes first, so what is overdue is on top`() {
        val summary = LastDone.summarise(
            listOf(Mark("a", at(1)), Mark("b", at(20)), Mark("c", at(5))),
            zone,
        )
        assertEquals(listOf("b", "c", "a"), summary.map { it.name })
    }

    @Test
    fun `average gap needs at least two different days`() {
        val once = LastDone.summarise(listOf(Mark("x", at(3))), zone).single()
        assertNull(once.averageGapDays)

        // Two taps on the same day are one occasion, not a gap of zero.
        val sameDay = LastDone.summarise(listOf(Mark("x", at(3, 9)), Mark("x", at(3, 18))), zone).single()
        assertNull(sameDay.averageGapDays)

        val weekly = LastDone.summarise(listOf(Mark("x", at(21)), Mark("x", at(14)), Mark("x", at(7))), zone).single()
        assertEquals(7, weekly.averageGapDays)
    }

    @Test
    fun `no events, no fact`() {
        assertNull(LastDone.funFact(emptyList(), today, Random(1)))
    }

    @Test
    fun `a single mark gives the plain last-time fact`() {
        val summary = LastDone.summarise(listOf(Mark("laundry", at(7))), zone)
        assertEquals(
            "Your last “laundry” was 7 days ago.",
            LastDone.funFact(summary, today, Random(1)),
        )
    }

    @Test
    fun `the same seed gives the same fact`() {
        val summary = LastDone.summarise(
            listOf(Mark("laundry", at(21)), Mark("laundry", at(7)), Mark("haircut", at(40))),
            zone,
        )
        assertEquals(
            LastDone.funFact(summary, today, Random(42)),
            LastDone.funFact(summary, today, Random(42)),
        )
    }

    @Test
    fun `with history, every kind of fact can come up`() {
        val summary = LastDone.summarise(
            listOf(Mark("laundry", at(30)), Mark("laundry", at(24)), Mark("laundry", at(18))),
            zone,
        )
        val seen = (0 until 500).mapNotNull { LastDone.funFact(summary, today, Random(it)) }.toSet()
        assertTrue(seen.any { it.startsWith("Your last") })
        assertTrue(seen.any { it.contains("3 times") })
        assertTrue(seen.any { it.contains("about every 6 days") })
        // 18 days since, against a usual 6: overdue.
        assertTrue(seen.any { it.contains("longer than usual") })
    }
}
