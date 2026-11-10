package dev.zakyymuh.timebox.core

import java.time.Instant
import java.time.LocalDate

/**
 * One thing to do today.
 *
 * Done-ness is an instant rather than a boolean so the list can later say *when*
 * something was ticked off without a migration. [position] is the order in the list.
 */
data class TodoItem(
    val id: Long,
    val position: Int,
    val text: String,
    val doneAt: Instant? = null,
    /** An emoji shown big on the item's card, or null for a plain card. */
    val icon: String? = null,
    /** A line or two of detail under the name, or null for none. */
    val description: String? = null,
    /** When "Working on it" was tapped; null if it never was. */
    val startedAt: Instant? = null,
) {
    val isDone: Boolean get() = doneAt != null

    /** Started and not yet finished. While true the card holds the screen until Done. */
    val isWorking: Boolean get() = startedAt != null && doneAt == null
}

/**
 * A day: an optional note about what the day is for, and the list of things to do.
 *
 * [items] is ordered by [TodoItem.position].
 */
data class DayList(
    val date: LocalDate,
    val note: String?,
    val items: List<TodoItem>,
)
