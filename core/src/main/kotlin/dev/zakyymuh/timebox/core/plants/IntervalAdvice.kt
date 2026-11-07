package dev.zakyymuh.timebox.core.plants

import java.time.temporal.ChronoUnit

/**
 * Learns the real rhythm from the owner's own waterings. After four waterings it takes
 * the median of the last five gaps — a median, so one holiday doesn't skew it — and
 * suggests it when it differs from the set interval by a day or more. A suggestion only:
 * nothing changes until the owner taps "Use".
 */
object IntervalAdvice {

    private const val MIN_WATERINGS = 4
    private const val LAST_GAPS = 5

    fun suggest(plant: Plant, waterings: List<Watering>): Int? {
        val interval = (plant.recurrence as? Recurrence.EveryDays)?.days ?: return null
        val dates = waterings.filter { it.plantId == plant.id }.map { it.date }.distinct().sorted()
        if (dates.size < MIN_WATERINGS) return null
        val gaps = dates.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
            .takeLast(LAST_GAPS)
            .sorted()
        val mid = gaps.size / 2
        // Even count: average the two middles, rounding half up.
        val median = if (gaps.size % 2 == 1) gaps[mid] else (gaps[mid - 1] + gaps[mid] + 1) / 2
        return median.takeIf { kotlin.math.abs(it - interval) >= 1 && it >= 1 }
    }
}
