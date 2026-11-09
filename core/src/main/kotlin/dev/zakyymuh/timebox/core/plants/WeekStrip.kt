package dev.zakyymuh.timebox.core.plants

import java.time.LocalDate

/** What a plant is doing on one day of the dashboard strip. */
sealed interface DayMark {
    /** A watering was logged that day. */
    data object Watered : DayMark

    /** Today only: needs water now, [daysLate] days after it was due. */
    data class Due(val daysLate: Int) : DayMark

    /** A day still to come that the schedule falls on. */
    data object Coming : DayMark
}

data class DayPlant(val plant: Plant, val mark: DayMark)

/** How a day pill shows watering: light blue once watered, a light green border while a plant is still to do. */
enum class DayBorder { Watered, ToDo, None }

data class StripDay(val date: LocalDate, val plants: List<DayPlant>) {
    val border: DayBorder
        get() = when {
            plants.any { it.mark == DayMark.Watered } -> DayBorder.Watered
            plants.isNotEmpty() -> DayBorder.ToDo
            else -> DayBorder.None
        }
}

/**
 * The dashboard's week: three days back, today, three days ahead. The past shows what was
 * actually watered; today what is due (late ones first); the future what the schedule says.
 */
object WeekStrip {

    const val SIDE = 3

    fun of(plants: List<Plant>, waterings: List<Watering>, today: LocalDate): List<StripDay> {
        val byId = plants.associateBy { it.id }
        return (-SIDE..SIDE).map { offset ->
            val date = today.plusDays(offset.toLong())
            val watered = waterings.filter { it.date == date }.mapNotNull { byId[it.plantId] }.distinct()
                .sortedBy { it.name }.map { DayPlant(it, DayMark.Watered) }
            val scheduled = when {
                offset < 0 -> emptyList()
                offset == 0 -> due(plants, waterings, today)
                else -> plants.filter { date in coming(it, waterings, today) }
                    .sortedBy { it.name }.map { DayPlant(it, DayMark.Coming) }
            }
            StripDay(date, scheduled + watered)
        }
    }

    private fun due(plants: List<Plant>, waterings: List<Watering>, today: LocalDate): List<DayPlant> =
        plants.mapNotNull { p ->
            (PlantSchedule.status(p, waterings, today) as? PlantStatus.Due)?.let { DayPlant(p, DayMark.Due(it.daysLate)) }
        }.sortedWith(compareByDescending<DayPlant> { (it.mark as DayMark.Due).daysLate }.thenBy { it.plant.name })

    /**
     * The coming dates within the strip. A plant due today or late is taken as watered
     * today, so its next run counts from today, as it will once it is tapped.
     */
    private fun coming(plant: Plant, waterings: List<Watering>, today: LocalDate): Set<LocalDate> {
        val next = PlantSchedule.nextDue(plant, waterings) ?: return emptySet()
        val end = today.plusDays(SIDE.toLong())
        val every = (plant.recurrence as? Recurrence.EveryDays)?.days?.toLong()
        if (every == null) return if (next.isAfter(today) && !next.isAfter(end)) setOf(next) else emptySet()
        var date = if (next.isAfter(today)) next else today.plusDays(every)
        val dates = mutableSetOf<LocalDate>()
        while (!date.isAfter(end)) {
            dates += date
            date = date.plusDays(every)
        }
        return dates
    }
}
