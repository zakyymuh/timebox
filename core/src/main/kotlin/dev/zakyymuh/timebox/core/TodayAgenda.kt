package dev.zakyymuh.timebox.core

import dev.zakyymuh.timebox.core.plants.PlantRow
import dev.zakyymuh.timebox.core.plants.PlantsOverview

/** One thing on the dashboard's Today list. */
sealed interface TodayEntry {
    /** A plant due or overdue today. */
    data class Water(val row: PlantRow) : TodayEntry

    /** One of today's items, not yet ticked. */
    data class Tick(val item: TodoItem) : TodayEntry
}

/**
 * Everything that still needs the owner today, on the front page: plants first, since
 * they cannot wait, most overdue first; then today's unticked items in list order.
 * Ticked items and plants that are not due yet are left out.
 */
object TodayAgenda {

    const val ALL_DONE = "All done for today 🌿"

    fun of(day: DayList, overview: PlantsOverview): List<TodayEntry> =
        overview.due.map { TodayEntry.Water(it) } +
            day.items.filterNot { it.isDone }.sortedBy { it.position }.map { TodayEntry.Tick(it) }

    fun ticked(text: String): String = "Ticked $text"
}
