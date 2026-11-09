package dev.zakyymuh.timebox.core.plants

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** The dashboard's "this month" card: how much watering so far, against last month. */
data class WateringMonth(val total: Int, val lastMonth: Int, val perPlant: List<Pair<Plant, Int>>) {

    companion object {

        fun of(plants: List<Plant>, waterings: List<Watering>, today: LocalDate): WateringMonth {
            val now = YearMonth.from(today)
            val thisMonth = waterings.filter { YearMonth.from(it.date) == now && !it.date.isAfter(today) }
            val byId = plants.associateBy { it.id }
            val perPlant = thisMonth.groupingBy { it.plantId }.eachCount()
                .mapNotNull { (id, count) -> byId[id]?.let { it to count } }
                .sortedWith(compareByDescending<Pair<Plant, Int>> { it.second }.thenBy { it.first.name })
            return WateringMonth(
                total = thisMonth.size,
                lastMonth = waterings.count { YearMonth.from(it.date) == now.minusMonths(1) },
                perPlant = perPlant,
            )
        }

        fun headline(month: WateringMonth): String =
            if (month.total == 1) "1 watering" else "${month.total} waterings"

        /** "3 more than October", "Same as October", "None in October". */
        fun compare(month: WateringMonth, today: LocalDate): String {
            val last = today.minusMonths(1).month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            val diff = month.total - month.lastMonth
            return when {
                month.lastMonth == 0 -> "None in $last"
                diff > 0 -> "$diff more than $last"
                diff < 0 -> "${-diff} fewer than $last"
                else -> "Same as $last"
            }
        }
    }
}
