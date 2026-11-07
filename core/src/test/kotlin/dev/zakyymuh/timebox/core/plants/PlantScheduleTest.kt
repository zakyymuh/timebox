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
    fun `status is Due 0 on the due day, Due 2 two days later, Upcoming 1 the day before`() {
        assertEquals(PlantStatus.Due(0), PlantSchedule.status(monstera, emptyList(), d(8)))
        assertEquals(PlantStatus.Due(2), PlantSchedule.status(monstera, emptyList(), d(10)))
        assertEquals(PlantStatus.Upcoming(1), PlantSchedule.status(monstera, emptyList(), today))
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
}
