package dev.zakyymuh.timebox.core.plants

import java.time.LocalDate
import java.time.YearMonth

enum class MarkKind { WATERED, PROJECTED }

data class Mark(val plantId: Long, val colour: PlantColour, val kind: MarkKind)

/**
 * The calendar's dots. Past is fact, future is projection: a solid dot wherever a
 * watering actually happened, and hollow dots from today onward wherever the schedule
 * currently expects one. Projections are recomputed from the latest watering every
 * time, so they move whenever you water; solid dots never do.
 */
object CalendarMarks {

    fun forMonth(
        month: YearMonth,
        plants: List<Plant>,
        waterings: List<Watering>,
        today: LocalDate,
    ): Map<LocalDate, List<Mark>> {
        val first = month.atDay(1)
        val last = month.atEndOfMonth()
        val marks = sortedMapOf<LocalDate, MutableList<Mark>>()
        fun add(date: LocalDate, plant: Plant, kind: MarkKind) {
            if (date < first || date > last) return
            marks.getOrPut(date) { mutableListOf() } += Mark(plant.id, plant.colour, kind)
        }

        for (plant in plants) {
            waterings.filter { it.plantId == plant.id }.forEach { add(it.date, plant, MarkKind.WATERED) }

            val due = PlantSchedule.nextDue(plant, waterings, today) ?: continue
            // An overdue plant is drawn on today, not back on the date it was missed.
            var date = if (due < today) today else due
            when (val r = plant.recurrence) {
                is Recurrence.Once -> add(date, plant, MarkKind.PROJECTED)
                is Recurrence.EveryDays -> while (date <= last) {
                    add(date, plant, MarkKind.PROJECTED)
                    date = date.plusDays(r.days.toLong())
                }
            }
        }
        return marks.mapValues { (_, list) -> list.sortedBy { it.plantId } }
    }
}
