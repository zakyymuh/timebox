package dev.zakyymuh.timebox.core

import dev.zakyymuh.timebox.core.plants.Plant
import dev.zakyymuh.timebox.core.plants.PlantColour
import dev.zakyymuh.timebox.core.plants.PlantCopy
import dev.zakyymuh.timebox.core.plants.PlantStatus
import dev.zakyymuh.timebox.core.plants.PlantsOverview
import dev.zakyymuh.timebox.core.plants.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TodayAgendaTest {

    private val today = LocalDate.of(2026, 11, 7)

    private fun plant(id: Long, name: String, every: Int, daysAgo: Long) =
        Plant(id, name, PlantColour.MOSS, Recurrence.EveryDays(every), today.minusDays(daysAgo))

    private val basil = plant(1, "Basil", 3, 3)        // due today
    // One-time, two days late: repeating plants skip a missed day instead.
    private val fern = Plant(2, "Fern", PlantColour.MOSS, Recurrence.Once(today.minusDays(2)), today.minusDays(7))
    private val monstera = plant(3, "Monstera", 7, 6)  // tomorrow

    private fun item(id: Long, position: Int, text: String, done: Boolean = false) =
        TodoItem(id, position, text, if (done) Instant.EPOCH else null)

    private fun day(vararg items: TodoItem) = DayList(today, null, items.toList())
    private fun overview(vararg plants: Plant) = PlantsOverview.of(plants.toList(), emptyList(), today)

    private fun describe(entries: List<TodayEntry>) = entries.map {
        when (it) {
            is TodayEntry.Water -> "water ${it.row.plant.name}"
            is TodayEntry.Tick -> "tick ${it.item.text}"
        }
    }

    @Test
    fun `plants come first, most overdue first, then unticked items by position`() {
        val entries = TodayAgenda.of(
            day(item(11, 1, "repotting"), item(10, 0, "cuci piring")),
            overview(basil, fern),
        )
        assertEquals(listOf("water Fern", "water Basil", "tick cuci piring", "tick repotting"), describe(entries))
    }

    @Test
    fun `ticked items and upcoming plants are left out`() {
        val entries = TodayAgenda.of(day(item(10, 0, "cuci piring", done = true), item(11, 1, "beberes")), overview(monstera))
        assertEquals(listOf("tick beberes"), describe(entries))
    }

    @Test
    fun `nothing written but a plant due gives just the plant`() {
        assertEquals(listOf("water Basil"), describe(TodayAgenda.of(day(), overview(basil))))
    }

    @Test
    fun `overdue plant keeps its days-late status`() {
        val water = TodayAgenda.of(day(), overview(fern)).single() as TodayEntry.Water
        assertEquals(PlantStatus.Due(2), water.row.status)
    }

    @Test
    fun `everything done gives an empty list`() {
        assertTrue(TodayAgenda.of(day(item(10, 0, "cuci piring", done = true)), overview(monstera)).isEmpty())
        assertEquals("All done for today 🌿", TodayAgenda.ALL_DONE)
    }

    @Test
    fun `ticked and watered copy`() {
        assertEquals("Ticked cuci piring", TodayAgenda.ticked("cuci piring"))
        assertEquals("Watered Basil", PlantCopy.watered("Basil"))
    }
}
