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

    /**
     * Where › goes from [viewing]: the next day with items, but never past today — and
     * today is always reachable from the past, even before anything is written on it.
     * Null when already on today.
     */
    fun nextStep(viewing: LocalDate, nextWithItems: LocalDate?, today: LocalDate): LocalDate? {
        if (!viewing.isBefore(today)) return null
        return if (nextWithItems != null && nextWithItems.isBefore(today)) nextWithItems else today
    }

    /** A day after today should never be reachable by stepping through the record. */
    fun isFuture(viewing: LocalDate, today: LocalDate): Boolean = viewing.isAfter(today)
}
