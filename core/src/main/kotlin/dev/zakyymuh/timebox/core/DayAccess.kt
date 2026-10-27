package dev.zakyymuh.timebox.core

import java.time.LocalDate

/**
 * Which day you are allowed to change.
 *
 * Only today. The record of earlier days is read-only, because yesterday is not a
 * backlog — an unfinished item from last Tuesday is a fact about last Tuesday, not a
 * task still waiting. If you still want it, you copy it forward to today, which is a
 * decision rather than an inheritance.
 */
object DayAccess {

    fun isEditable(viewing: LocalDate, today: LocalDate): Boolean = viewing == today

    fun isToday(viewing: LocalDate, today: LocalDate): Boolean = viewing == today

    /** A day after today should never be reachable by stepping through the record. */
    fun isFuture(viewing: LocalDate, today: LocalDate): Boolean = viewing.isAfter(today)
}
