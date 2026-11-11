package dev.zakyymuh.timebox.core.plants

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The owner's rule: the next watering is counted from the day you actually watered,
 * never from the date it was due. A repeating plant whose day passes without water is
 * skipped, not late: it simply comes round again one interval later. A one-time plant
 * is never skipped; it stays due, as a reminder, until it is watered.
 */
object PlantSchedule {

    /**
     * Next due date as of [today], or null for a one-time plant that has been watered.
     * Missed days of a repeating plant are skipped, so this is never before [today] for one.
     */
    fun nextDue(plant: Plant, waterings: List<Watering>, today: LocalDate): LocalDate? {
        val due = nextDue(plant, waterings) ?: return null
        val every = (plant.recurrence as? Recurrence.EveryDays)?.days?.toLong() ?: return due
        if (!due.isBefore(today)) return due
        val missed = ChronoUnit.DAYS.between(due, today)
        return due.plusDays((missed + every - 1) / every * every)
    }

    /**
     * The date the schedule first fell on, before any skipping: counted from the last real
     * watering, or from "still moist" when that is later. Null for a watered one-time plant.
     */
    fun nextDue(plant: Plant, waterings: List<Watering>): LocalDate? {
        val scheduled = scheduledDue(plant, waterings) ?: return null
        val snooze = plant.snoozedUntil
        return if (snooze != null && snooze.isAfter(scheduled)) snooze else scheduled
    }

    private fun scheduledDue(plant: Plant, waterings: List<Watering>): LocalDate? {
        val latest = waterings.filter { it.plantId == plant.id }.maxOfOrNull { it.date }
        return when (val r = plant.recurrence) {
            // Only waterings after the day it became one-time count, so switching a
            // plant with history to "once" does not finish it on the spot.
            is Recurrence.Once -> if (latest == null || !latest.isAfter(plant.anchor)) r.on else null
            is Recurrence.EveryDays -> {
                val base = if (latest != null && latest.isAfter(plant.anchor)) latest else plant.anchor
                base.plusDays(r.days.toLong())
            }
        }
    }

    fun status(plant: Plant, waterings: List<Watering>, today: LocalDate): PlantStatus {
        val due = nextDue(plant, waterings, today) ?: return PlantStatus.Done
        val days = ChronoUnit.DAYS.between(today, due).toInt()
        return if (days <= 0) PlantStatus.Due(-days) else PlantStatus.Upcoming(days)
    }
}
