package dev.zakyymuh.timebox.core.plants

import dev.zakyymuh.timebox.core.DaySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlantsOverviewTest {

    private val today = LocalDate.of(2026, 11, 7)

    private fun plant(id: Long, name: String, every: Int, anchor: LocalDate) =
        Plant(id, name, PlantColour.MOSS, Recurrence.EveryDays(every), anchor)

    // Basil due today, Fern 2 days late, Monstera tomorrow, Snake plant in 4 days.
    private val basil = plant(1, "Basil", 3, today.minusDays(3))
    private val fern = plant(2, "Fern", 5, today.minusDays(7))
    private val monstera = plant(3, "Monstera", 7, today.minusDays(6))
    private val snake = plant(4, "Snake plant", 14, today.minusDays(10))

    private fun overview(vararg plants: Plant, waterings: List<Watering> = emptyList()) =
        PlantsOverview.of(plants.toList(), waterings, today)

    @Test
    fun `due sorts most overdue first and upcoming soonest first`() {
        val o = overview(snake, basil, monstera, fern)
        assertEquals(listOf("Fern", "Basil"), o.due.map { it.plant.name })
        assertEquals(listOf("Monstera", "Snake plant"), o.upcoming.map { it.plant.name })
    }

    @Test
    fun `next is the soonest upcoming plant only when nothing is due`() {
        assertNull(overview(basil, monstera).next)
        assertEquals("Monstera", overview(snake, monstera).next?.plant?.name)
    }

    @Test
    fun `a watered one-time plant appears in neither list`() {
        val once = Plant(9, "Cactus", PlantColour.OCHRE, Recurrence.Once(today), anchor = today.minusDays(1))
        val o = overview(once, waterings = listOf(Watering(1, 9, today)))
        assertTrue(o.due.isEmpty() && o.upcoming.isEmpty())
        assertTrue(o.isEmpty)
    }

    @Test
    fun `dashboard copy`() {
        assertEquals("No plants yet", PlantCopy.dashboard(overview()))
        assertEquals("1 due today", PlantCopy.dashboard(overview(basil, monstera)))
        assertEquals("2 due today", PlantCopy.dashboard(overview(basil, fern)))
        assertEquals("Next: Monstera tomorrow", PlantCopy.dashboard(overview(monstera, snake)))
        assertEquals("Next: Snake plant in 4 days", PlantCopy.dashboard(overview(snake)))
    }

    @Test
    fun `notification copy`() {
        assertNull(PlantCopy.notification(overview(monstera)))
        assertEquals("Basil needs water", PlantCopy.notification(overview(basil, monstera)))
        assertEquals("2 plants need water", PlantCopy.notification(overview(basil, fern)))
    }

    @Test
    fun `whenLabel copy`() {
        assertEquals("today", PlantCopy.whenLabel(PlantStatus.Due(0)))
        assertEquals("1 day late", PlantCopy.whenLabel(PlantStatus.Due(1)))
        assertEquals("3 days late", PlantCopy.whenLabel(PlantStatus.Due(3)))
        assertEquals("tomorrow", PlantCopy.whenLabel(PlantStatus.Upcoming(1)))
        assertEquals("in 4 days", PlantCopy.whenLabel(PlantStatus.Upcoming(4)))
        assertEquals("done", PlantCopy.whenLabel(PlantStatus.Done))
    }

    @Test
    fun `interval copy`() {
        assertEquals("every 3 days", PlantCopy.interval(Recurrence.EveryDays(3)))
        assertEquals("every day", PlantCopy.interval(Recurrence.EveryDays(1)))
        assertEquals("once", PlantCopy.interval(Recurrence.Once(today)))
    }

    @Test
    fun `habit copy`() {
        assertEquals("2 / 5 done", PlantCopy.habit(DaySummary(2, 5)))
        assertEquals("Nothing written yet", PlantCopy.habit(DaySummary(0, 0)))
    }

    @Test
    fun `last watered copy`() {
        assertEquals("Watered today", PlantCopy.lastWatered(today, today))
        assertEquals("Watered yesterday", PlantCopy.lastWatered(today.minusDays(1), today))
        assertEquals("Watered 3 days ago", PlantCopy.lastWatered(today.minusDays(3), today))
        assertEquals("Not watered yet", PlantCopy.lastWatered(null, today))
    }

    @Test
    fun `checked, usually and use copy`() {
        assertEquals("Checked Basil", PlantCopy.checked("Basil"))
        assertEquals("You usually water every 8 days", PlantCopy.usually(8))
        assertEquals("Use 8", PlantCopy.use(8))
    }

    @Test
    fun `PlantRow carries last watered and suggested interval`() {
        val waterings = listOf(1L to 20L, 2L to 12L, 3L to 4L, 4L to 0L).map { (id, ago) -> Watering(id, 1, today.minusDays(ago)) }
        val row = PlantsOverview.of(listOf(basil), waterings, today).let { it.due + it.upcoming }.single()
        assertEquals(today, row.lastWatered)
        assertEquals(8, row.suggestedInterval)
    }

    @Test
    fun `last watered falls back to the date given when the plant was added`() {
        // Basil was added saying "last watered 3 days ago" and hasn't been tapped since.
        val row = PlantsOverview.of(listOf(basil), emptyList(), today).let { it.due + it.upcoming }.single()
        assertEquals(today.minusDays(3), row.lastWatered)
    }

    @Test
    fun `a one-time plant with no waterings has not been watered`() {
        val once = Plant(9, "Cactus", PlantColour.OCHRE, Recurrence.Once(today.plusDays(2)), anchor = today.minusDays(1))
        val row = PlantsOverview.of(listOf(once), emptyList(), today).upcoming.single()
        assertEquals(null, row.lastWatered)
    }
}
