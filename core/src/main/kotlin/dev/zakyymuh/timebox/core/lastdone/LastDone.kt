package dev.zakyymuh.timebox.core.lastdone

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/** One time something was done: "laundry, at this moment". */
data class Mark(val name: String, val at: Instant)

/** Everything known about one thing that gets done now and then. */
data class Event(
    /** Lowercased and trimmed, so "Laundry" and "laundry " are the same thing. */
    val key: String,
    /** The spelling used most recently. */
    val name: String,
    val lastAt: Instant,
    val lastDay: LocalDate,
    val firstDay: LocalDate,
    /** Every tap, including two on the same day. */
    val count: Int,
    /** Whole days between occasions on average; null until it has happened on two different days. */
    val averageGapDays: Int?,
) {
    fun daysSince(today: LocalDate): Long = ChronoUnit.DAYS.between(lastDay, today).coerceAtLeast(0)
}

/**
 * "When did I last …?" Things that happen irregularly — laundry, a haircut, changing the
 * bedsheets — logged with one tap, and read back as how long ago.
 */
object LastDone {

    fun key(name: String): String = name.trim().lowercase()

    /** Calendar days, so 23:50 last night is "yesterday" even at 00:10. */
    fun ago(last: LocalDate, today: LocalDate): String {
        val days = ChronoUnit.DAYS.between(last, today).coerceAtLeast(0)
        return when {
            days == 0L -> "today"
            days == 1L -> "yesterday"
            days <= 45 -> "$days days ago"
            days < 365 -> "about ${(days + 15) / 30} months ago"
            else -> "over a year ago"
        }
    }

    /** One [Event] per name, the longest ago first so what is overdue sits on top. */
    fun summarise(marks: List<Mark>, zone: ZoneId): List<Event> =
        marks.filter { it.name.isNotBlank() }
            .groupBy { key(it.name) }
            .map { (key, group) ->
                val sorted = group.sortedBy { it.at }
                val days = sorted.map { it.at.atZone(zone).toLocalDate() }.distinct()
                Event(
                    key = key,
                    name = sorted.last().name.trim(),
                    lastAt = sorted.last().at,
                    lastDay = days.last(),
                    firstDay = days.first(),
                    count = sorted.size,
                    averageGapDays = if (days.size < 2) null else {
                        val span = ChronoUnit.DAYS.between(days.first(), days.last())
                        Math.round(span.toDouble() / (days.size - 1)).toInt()
                    },
                )
            }
            .sortedWith(compareBy<Event> { it.lastAt }.thenBy { it.key })

    /** A random line about a random event, or null when nothing has been logged yet. */
    fun funFact(events: List<Event>, today: LocalDate, random: Random): String? {
        if (events.isEmpty()) return null
        val e = events[random.nextInt(events.size)]
        val name = "“${e.name}”"
        val since = e.daysSince(today)
        val gap = e.averageGapDays

        val facts = buildList {
            add("Your last $name was ${ago(e.lastDay, today)}.")
            if (e.count >= 2) {
                val sinceFirst = ChronoUnit.DAYS.between(e.firstDay, today)
                add("You've done $name ${e.count} times since ${if (sinceFirst == 0L) "today" else "$sinceFirst days ago"}.")
            }
            if (gap != null && gap > 0) {
                add("You usually do $name about every $gap ${if (gap == 1) "day" else "days"}.")
                if (since > gap * 3 / 2) {
                    add("It's been $since days since your last $name — longer than usual.")
                } else if (since < gap) {
                    val left = gap - since
                    add("Next $name is due in about $left ${if (left == 1L) "day" else "days"}.")
                }
            }
        }
        return facts[random.nextInt(facts.size)]
    }
}
