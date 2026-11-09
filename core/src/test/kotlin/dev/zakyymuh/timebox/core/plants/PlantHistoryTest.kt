package dev.zakyymuh.timebox.core.plants

import dev.zakyymuh.timebox.core.collection.DexEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PlantHistoryTest {

    private val today = LocalDate.of(2026, 11, 9) // a Monday
    private fun d(day: Int, month: Int = 11) = LocalDate.of(2026, month, day)

    private val basil = Plant(1, "Basil", PlantColour.MOSS, Recurrence.EveryDays(3), anchor = d(1, 10))
    private val aloe = Plant(2, "Aloe", PlantColour.OCHRE, Recurrence.EveryDays(7), anchor = d(1, 10))

    private fun dex(id: Long, name: String, acquired: LocalDate?, owned: Boolean = true) =
        DexEntry(id = id, number = id.toInt(), name = name, species = "", owned = owned,
            photo = null, acquired = acquired, note = "")

    private val waterings = listOf(
        Watering(1, 1, d(9)),
        Watering(2, 2, d(9)),
        Watering(3, 1, d(8)),
        Watering(4, 1, d(2)),
        Watering(5, 2, d(14, 10)),
        Watering(6, 99, d(9)), // a deleted plant
    )
    private val entries = listOf(dex(1, "Monstera", d(20, 10)), dex(2, "Fig", d(1), owned = false), dex(3, "Jade", null))

    private val events = PlantHistory.events(listOf(basil, aloe), waterings, entries)

    @Test
    fun `waterings and plants joining the collection, newest first`() {
        assertEquals(
            listOf(
                d(9) to "Watered Aloe",
                d(9) to "Watered Basil",
                d(8) to "Watered Basil",
                d(2) to "Watered Basil",
                d(20, 10) to "Monstera joined your collection",
                d(14, 10) to "Watered Aloe",
            ),
            events.map { it.date to it.text },
        )
    }

    @Test
    fun `grouped by Today, Yesterday, days this month, then whole months`() {
        val groups = PlantHistory.group(events, today)
        assertEquals(listOf("Today", "Yesterday", "Mon 2 Nov", "October 2026"), groups.map { it.title })
        assertEquals(2, groups.last().events.size)
    }

    @Test
    fun `search matches the plant or the event, ignoring case, and keeps the dates`() {
        val found = PlantHistory.search(events, "  ALOE ")
        assertEquals(listOf("Today", "October 2026"), PlantHistory.group(found, today).map { it.title })
        assertEquals(listOf(HistoryKind.Joined), PlantHistory.search(events, "joined").map { it.kind })
        assertEquals(events, PlantHistory.search(events, ""))
    }

    @Test
    fun `what an empty list says`() {
        assertEquals("Nothing has happened yet", PlantHistory.emptyMessage(emptyList(), ""))
        assertEquals("Nothing matches ‘cactus’", PlantHistory.emptyMessage(emptyList(), " cactus "))
        assertNull(PlantHistory.emptyMessage(events, "aloe"))
    }
}
