package dev.zakyymuh.timebox.core.plants

import dev.zakyymuh.timebox.core.DaySummary
import java.time.LocalDate

data class PlantRow(
    val plant: Plant,
    val status: PlantStatus,
    val nextDue: LocalDate?,
    val lastWatered: LocalDate? = null,
    /** A better interval learned from history, when there is one. */
    val suggestedInterval: Int? = null,
)

/** What the plant page shows: what needs you now, then what is coming. */
data class PlantsOverview(val due: List<PlantRow>, val upcoming: List<PlantRow>) {

    /** Shown as the NEXT WATERING card, only when nothing is due. */
    val next: PlantRow? get() = if (due.isEmpty()) upcoming.firstOrNull() else null

    val isEmpty: Boolean get() = due.isEmpty() && upcoming.isEmpty()

    companion object {
        /**
         * The latest logged watering — or, for a repeating plant never tapped yet, the
         * "last watered" date given when it was added. (A one-time plant's anchor is not a
         * watering, so it stays null.)
         */
        private fun lastWatered(p: Plant, waterings: List<Watering>): LocalDate? {
            val logged = waterings.filter { it.plantId == p.id }.maxOfOrNull { it.date }
            val given = p.anchor.takeIf { p.recurrence is Recurrence.EveryDays }
            return listOfNotNull(logged, given).maxOrNull()
        }

        fun of(plants: List<Plant>, waterings: List<Watering>, today: LocalDate): PlantsOverview {
            val rows = plants.map { p ->
                PlantRow(
                    plant = p,
                    status = PlantSchedule.status(p, waterings, today),
                    nextDue = PlantSchedule.nextDue(p, waterings),
                    lastWatered = lastWatered(p, waterings),
                    suggestedInterval = IntervalAdvice.suggest(p, waterings),
                )
            }
            val due = rows.filter { it.status is PlantStatus.Due }
                .sortedWith(compareByDescending<PlantRow> { (it.status as PlantStatus.Due).daysLate }.thenBy { it.plant.name })
            val upcoming = rows.filter { it.status is PlantStatus.Upcoming }
                .sortedWith(compareBy<PlantRow> { (it.status as PlantStatus.Upcoming).inDays }.thenBy { it.plant.name })
            return PlantsOverview(due, upcoming)
        }
    }
}

/** Every user-facing sentence about plants, in one place so it can be tested. */
object PlantCopy {

    fun whenLabel(status: PlantStatus): String = when (status) {
        is PlantStatus.Due -> when (status.daysLate) {
            0 -> "today"
            1 -> "1 day late"
            else -> "${status.daysLate} days late"
        }
        is PlantStatus.Upcoming -> if (status.inDays == 1) "tomorrow" else "in ${status.inDays} days"
        PlantStatus.Done -> "done"
    }

    fun interval(recurrence: Recurrence): String = when (recurrence) {
        is Recurrence.EveryDays -> if (recurrence.days == 1) "every day" else "every ${recurrence.days} days"
        is Recurrence.Once -> "once"
    }

    /** Beside a plant in the dashboard's day list. */
    fun mark(mark: DayMark): String = when (mark) {
        DayMark.Watered -> "watered"
        is DayMark.Due -> if (mark.daysLate == 0) "due today" else whenLabel(PlantStatus.Due(mark.daysLate))
        DayMark.Coming -> "due"
    }

    /** The dashboard's day list when nothing falls on that day. */
    fun emptyDay(date: LocalDate, today: LocalDate): String =
        if (date.isBefore(today)) "Nothing watered" else "Nothing due"

    fun notification(overview: PlantsOverview): String? = when (overview.due.size) {
        0 -> null
        1 -> "${overview.due.single().plant.name} needs water"
        else -> "${overview.due.size} plants need water"
    }

    fun lastWatered(date: LocalDate?, today: LocalDate): String {
        if (date == null) return "Not watered yet"
        return when (val days = java.time.temporal.ChronoUnit.DAYS.between(date, today).toInt()) {
            0 -> "Watered today"
            1 -> "Watered yesterday"
            else -> "Watered $days days ago"
        }
    }

    /** The Undo snackbar after "Still moist". */
    fun checked(name: String): String = "Checked $name"

    fun usually(days: Int): String = "You usually water every $days days"

    fun use(days: Int): String = "Use $days"

    /** The Undo snackbar after watering. */
    fun watered(name: String): String = "Watered $name"

    fun habit(summary: DaySummary): String =
        if (summary.isEmpty) "Nothing written yet" else "${summary.done} / ${summary.total} done"
}
