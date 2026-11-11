package dev.zakyymuh.timebox.core.plants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PlantScheduleTest {

    private val today = LocalDate.of(2026, 11, 7)
    private fun d(day: Int, month: Int = 11) = LocalDate.of(2026, month, day)

    private val monstera = Plant(
        id = 1, name = "Monstera", colour = PlantColour.MOSS,
        recurrence = Recurrence.EveryDays(7), anchor = d(1),
    )

    private fun watered(vararg days: LocalDate, plantId: Long = 1) =
        days.mapIndexed { i, date -> Watering(id = i + 1L, plantId = plantId, date = date) }

    @Test
    fun `next due counts from the anchor when never watered`() {
        assertEquals(d(8), PlantSchedule.nextDue(monstera, emptyList()))
    }

    @Test
    fun `next due counts from the day actually watered, not the old due date`() {
        // Owner's case: watered on Thursday, two days after it was due.
        assertEquals(d(12), PlantSchedule.nextDue(monstera, watered(d(5))))
    }

    @Test
    fun `watering early also restarts the clock`() {
        assertEquals(d(10), PlantSchedule.nextDue(monstera, watered(d(3))))
    }

    @Test
    fun `the latest of several waterings is the one that counts`() {
        assertEquals(d(12), PlantSchedule.nextDue(monstera, watered(d(5), d(2))))
    }

    @Test
    fun `a watering older than the anchor is ignored`() {
        assertEquals(d(8), PlantSchedule.nextDue(monstera, watered(d(20, 10))))
    }

    @Test
    fun `other plants' waterings do not count`() {
        assertEquals(d(8), PlantSchedule.nextDue(monstera, watered(d(5), plantId = 99)))
    }

    @Test
    fun `changing the interval re-plans from the latest watering`() {
        val every3 = monstera.copy(recurrence = Recurrence.EveryDays(3))
        assertEquals(d(8), PlantSchedule.nextDue(every3, watered(d(5))))
    }

    @Test
    fun `status is Due 0 on the due day and Upcoming 1 the day before`() {
        assertEquals(PlantStatus.Due(0), PlantSchedule.status(monstera, emptyList(), d(8)))
        assertEquals(PlantStatus.Upcoming(1), PlantSchedule.status(monstera, emptyList(), today))
    }

    @Test
    fun `a missed day is skipped, so the plant comes round one interval later`() {
        // Due on the 8th, not watered: on the 9th it is due the 15th, not a day late.
        assertEquals(PlantStatus.Upcoming(6), PlantSchedule.status(monstera, emptyList(), d(9)))
        assertEquals(d(15), PlantSchedule.nextDue(monstera, emptyList(), d(9)))
        // The 15th itself is a due day again, and two misses later it is the 29th.
        assertEquals(PlantStatus.Due(0), PlantSchedule.status(monstera, emptyList(), d(15)))
        assertEquals(d(29), PlantSchedule.nextDue(monstera, emptyList(), d(23)))
    }

    @Test
    fun `an every-day plant missed yesterday is due today`() {
        val daily = monstera.copy(recurrence = Recurrence.EveryDays(1))
        assertEquals(PlantStatus.Due(0), PlantSchedule.status(daily, watered(d(3)), today))
    }

    @Test
    fun `watering the day after a skip restarts from that day`() {
        assertEquals(d(16), PlantSchedule.nextDue(monstera, watered(d(9)), d(9)))
    }

    @Test
    fun `still moist for some days is the new due day, and the interval runs from there`() {
        // Due the 8th; "still moist, water in 3 days" → the 11th, then the 18th if skipped.
        val moist = monstera.copy(snoozedUntil = d(11))
        assertEquals(PlantStatus.Upcoming(3), PlantSchedule.status(moist, emptyList(), d(8)))
        assertEquals(PlantStatus.Due(0), PlantSchedule.status(moist, emptyList(), d(11)))
        assertEquals(d(18), PlantSchedule.nextDue(moist, emptyList(), d(12)))
    }

    @Test
    fun `one-time plant is Due after its date until watered, then Done`() {
        val once = monstera.copy(recurrence = Recurrence.Once(d(4)))
        assertEquals(PlantStatus.Due(3), PlantSchedule.status(once, emptyList(), today))
        assertEquals(PlantStatus.Done, PlantSchedule.status(once, watered(d(6)), today))
        assertNull(PlantSchedule.nextDue(once, watered(d(6))))
    }

    @Test
    fun `one-time plant watered early is Done too`() {
        val once = monstera.copy(recurrence = Recurrence.Once(d(10)))
        assertEquals(PlantStatus.Done, PlantSchedule.status(once, watered(d(7)), today))
    }

    @Test
    fun `one-time plant in the future is Upcoming`() {
        val once = monstera.copy(recurrence = Recurrence.Once(d(10)))
        assertEquals(PlantStatus.Upcoming(3), PlantSchedule.status(once, emptyList(), today))
    }

    @Test
    fun `a one-time plant only counts waterings on or after its anchor`() {
        // Basil was watered on the 5th as a repeating plant, then switched to "once on
        // the 20th" today (the 7th): that old watering must not finish the new one-time job.
        val once = monstera.copy(recurrence = Recurrence.Once(d(20)), anchor = d(7))
        assertEquals(PlantStatus.Upcoming(13), PlantSchedule.status(once, watered(d(5)), today))
        // Watered earlier the same day it was switched: dates can't order those, and
        // keeping the plant listed is the safe side.
        assertEquals(PlantStatus.Upcoming(13), PlantSchedule.status(once, watered(d(7)), today))
        assertEquals(PlantStatus.Done, PlantSchedule.status(once, watered(d(8)), d(8)))
    }

    @Test
    fun `snooze moves next due to tomorrow when later than the schedule`() {
        // Due today (anchor 1st + 7 = 8th); checked on the 8th: still moist → 9th.
        val snoozed = monstera.copy(snoozedUntil = d(9))
        assertEquals(d(9), PlantSchedule.nextDue(snoozed, emptyList()))
        assertEquals(PlantStatus.Upcoming(1), PlantSchedule.status(snoozed, emptyList(), d(8)))
    }

    @Test
    fun `snooze earlier than the schedule changes nothing`() {
        assertEquals(d(8), PlantSchedule.nextDue(monstera.copy(snoozedUntil = d(5)), emptyList()))
    }

    @Test
    fun `snoozing again next day moves one more day`() {
        assertEquals(d(10), PlantSchedule.nextDue(monstera.copy(snoozedUntil = d(10)), emptyList()))
    }

    @Test
    fun `one-time plant respects snooze until done`() {
        val once = monstera.copy(recurrence = Recurrence.Once(d(4)), snoozedUntil = d(8))
        assertEquals(d(8), PlantSchedule.nextDue(once, emptyList()))
        assertNull(PlantSchedule.nextDue(once, watered(d(8))))
    }
}
