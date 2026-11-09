package dev.zakyymuh.timebox.core.plants

import dev.zakyymuh.timebox.core.collection.DexEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class HistoryKind { Watered, Joined }

/** One thing that happened: "Watered Basil" on a day. [subject] is the plant's name. */
data class HistoryEvent(val date: LocalDate, val subject: String, val text: String, val kind: HistoryKind)

/** A header ("Today", "Mon 2 Nov", "October 2026") and its events, newest first. */
data class HistoryGroup(val title: String, val events: List<HistoryEvent>)

/**
 * The history behind the dashboard's event tile: every watering and every plant that
 * joined the collection, newest first. Photos are not dated, so they cannot be placed.
 */
object PlantHistory {

    private val day = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
    private val month = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    fun events(plants: List<Plant>, waterings: List<Watering>, entries: List<DexEntry>): List<HistoryEvent> {
        val names = plants.associate { it.id to it.name }
        val watered = waterings.sortedByDescending { it.id }.mapNotNull { w ->
            names[w.plantId]?.let { HistoryEvent(w.date, it, "Watered $it", HistoryKind.Watered) }
        }
        val joined = entries.filter { it.owned && it.acquired != null }.sortedByDescending { it.id }
            .map { HistoryEvent(it.acquired!!, it.name, "${it.name} joined your collection", HistoryKind.Joined) }
        // Stable sort: within a day, the order above (newest logged first) is kept.
        return (watered + joined).sortedByDescending { it.date }
    }

    /** "Today", "Yesterday", a day this month, else the whole month. */
    fun group(events: List<HistoryEvent>, today: LocalDate): List<HistoryGroup> =
        events.groupBy { title(it.date, today) }.map { (title, list) -> HistoryGroup(title, list) }

    private fun title(date: LocalDate, today: LocalDate): String = when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.year == today.year && date.month == today.month -> date.format(day)
        else -> date.format(month)
    }

    /** By plant or by what happened, ignoring case. A blank query keeps everything. */
    fun search(events: List<HistoryEvent>, query: String): List<HistoryEvent> {
        val q = query.trim()
        if (q.isEmpty()) return events
        return events.filter { it.subject.contains(q, ignoreCase = true) || it.text.contains(q, ignoreCase = true) }
    }

    /** What to show instead of the list, or null when there is something to show. */
    fun emptyMessage(found: List<HistoryEvent>, query: String): String? = when {
        found.isNotEmpty() -> null
        query.isBlank() -> "Nothing has happened yet"
        else -> "Nothing matches ‘${query.trim()}’"
    }
}
