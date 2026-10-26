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
) {
    val isDone: Boolean get() = doneAt != null
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
