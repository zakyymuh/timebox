package dev.zakyymuh.timebox.core.plants

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The owner's rule: the next watering is counted from the day you actually watered,
 * never from the date it was due. Water two days late and the whole schedule slides
 * two days; that is the point, since what matters is how long the soil has been dry.
 */
object PlantSchedule {

    /** Next due date, or null for a one-time plant that has been watered. */
    fun nextDue(plant: Plant, waterings: List<Watering>): LocalDate? {
        val latest = waterings.filter { it.plantId == plant.id }.maxOfOrNull { it.date }
        return when (val r = plant.recurrence) {
            is Recurrence.Once -> if (latest == null) r.on else null
            is Recurrence.EveryDays -> {
                val base = if (latest != null && latest.isAfter(plant.anchor)) latest else plant.anchor
                base.plusDays(r.days.toLong())
            }
        }
    }

    fun status(plant: Plant, waterings: List<Watering>, today: LocalDate): PlantStatus {
        val due = nextDue(plant, waterings) ?: return PlantStatus.Done
        val days = ChronoUnit.DAYS.between(today, due).toInt()
        return if (days <= 0) PlantStatus.Due(-days) else PlantStatus.Upcoming(days)
    }
}
