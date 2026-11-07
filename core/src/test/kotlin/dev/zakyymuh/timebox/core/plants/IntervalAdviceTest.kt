package dev.zakyymuh.timebox.core.plants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class IntervalAdviceTest {

    private val start = LocalDate.of(2026, 9, 1)
    private val every4 = Plant(1, "Greenovia", PlantColour.MOSS, Recurrence.EveryDays(4), start)

    /** Waterings separated by [gaps] days, starting on [start]. */
    private fun history(vararg gaps: Long): List<Watering> {
        var date = start
        val dates = mutableListOf(date)
        gaps.forEach { date = date.plusDays(it); dates += date }
        return dates.mapIndexed { i, d -> Watering(i + 1L, 1, d) }
    }

    @Test
    fun `advice needs four waterings`() {
        assertNull(IntervalAdvice.suggest(every4, history(8, 8)))
    }

    @Test
    fun `advice is the median of the last five gaps`() {
        assertEquals(8, IntervalAdvice.suggest(every4, history(7, 8, 8, 9)))
    }

    @Test
    fun `only the last five gaps count`() {
        assertEquals(8, IntervalAdvice.suggest(every4, history(2, 2, 2, 8, 8, 8, 9, 7)))
    }

    @Test
    fun `advice ignores one holiday gap`() {
        assertEquals(7, IntervalAdvice.suggest(every4, history(7, 7, 30, 7)))
    }

    @Test
    fun `an even count takes the upper middle, rounded half up`() {
        // gaps 7, 8 → median 7.5 → 8
        assertEquals(8, IntervalAdvice.suggest(every4, history(7, 7, 8, 8)))
    }

    @Test
    fun `no advice when within a day of the interval`() {
        val every8 = every4.copy(recurrence = Recurrence.EveryDays(8))
        assertNull(IntervalAdvice.suggest(every8, history(7, 8, 8, 9)))
        // Median 7 against a 7-day interval: nothing to suggest.
        val every7 = every4.copy(recurrence = Recurrence.EveryDays(7))
        assertNull(IntervalAdvice.suggest(every7, history(7, 7, 7, 8)))
    }

    @Test
    fun `no advice for one-time plants or other plants' waterings`() {
        assertNull(IntervalAdvice.suggest(every4.copy(recurrence = Recurrence.Once(start)), history(8, 8, 8)))
        assertNull(IntervalAdvice.suggest(every4.copy(id = 2), history(8, 8, 8)))
    }

    @Test
    fun `no advice beyond the 90-day limit the form allows`() {
        // A cactus watered every ~100 days: "Use 100" would be rejected on save.
        assertNull(IntervalAdvice.suggest(every4, history(100, 100, 100)))
    }
}
