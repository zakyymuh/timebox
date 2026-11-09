package dev.zakyymuh.timebox.core.plants

import dev.zakyymuh.timebox.core.collection.DexEntry
import dev.zakyymuh.timebox.core.lastdone.LastDone
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * The dashboard's random line about something that happened: a watering, a plant joining
 * the collection, its photos. Like Daily's "Your last laundry was 7 days ago".
 */
object PlantEvents {

    /** Every line there is something to say for; empty until anything has happened. */
    fun lines(plants: List<Plant>, waterings: List<Watering>, entries: List<DexEntry>, today: LocalDate): List<String> =
        buildList {
            val byPlant = waterings.groupBy { it.plantId }
            plants.sortedBy { it.name }.forEach { p ->
                val days = byPlant[p.id]?.map { it.date } ?: return@forEach
                add("You last watered ${p.name} ${LastDone.ago(days.max(), today)}.")
                if (days.size >= 2) add("You've watered ${p.name} ${days.size} times.")
            }

            val arrived = entries.filter { it.owned && it.acquired != null }.sortedBy { it.acquired }
            arrived.firstOrNull()?.let {
                add("Your oldest plant is ${it.name}, ${with(it.acquired!!, today)} with you.")
            }
            arrived.forEach { add("You got ${it.name} ${LastDone.ago(it.acquired!!, today)}.") }

            val photos = entries.count { it.photo != null }
            if (photos > 0) add("Your collection has $photos ${if (photos == 1) "photo" else "photos"}.")
        }

    /** One line at random, or null when there is nothing to say yet. */
    fun random(plants: List<Plant>, waterings: List<Watering>, entries: List<DexEntry>, today: LocalDate, random: Random): String? =
        lines(plants, waterings, entries, today).takeIf { it.isNotEmpty() }?.random(random)

    /** How long a plant has been around: "12 days", "about 3 months", "over a year". */
    private fun with(since: LocalDate, today: LocalDate): String {
        val days = ChronoUnit.DAYS.between(since, today).coerceAtLeast(0)
        return when {
            days <= 1L -> "1 day"
            days <= 45 -> "$days days"
            days < 365 -> "about ${(days + 15) / 30} months"
            else -> "over a year"
        }
    }
}
