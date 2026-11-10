package dev.zakyymuh.timebox.core

import java.time.Instant
import java.time.LocalDate

/** Operations on a day's list. Pure: every function returns a new value. */
object Items {

    /** Ticks an item off, or un-ticks it if it was already done. */
    fun toggle(day: DayList, itemId: Long, now: Instant): DayList =
        day.copy(
            items = day.items.map {
                if (it.id != itemId) it else it.copy(doneAt = if (it.isDone) null else now)
            },
        )

    fun add(day: DayList, text: String): DayList =
        day.copy(items = renumber(day.items + TodoItem(0L, day.items.size, text.trim())))

    fun remove(day: DayList, itemId: Long): DayList =
        day.copy(items = renumber(day.items.filterNot { it.id == itemId }))

    fun rename(day: DayList, itemId: Long, text: String): DayList =
        day.copy(items = day.items.map { if (it.id == itemId) it.copy(text = text.trim()) else it })

    /**
     * Moves the item at [fromIndex] to [toIndex] and renumbers, so positions stay
     * contiguous from zero and list order is always day order.
     */
    fun reorder(items: List<TodoItem>, fromIndex: Int, toIndex: Int): List<TodoItem> {
        if (fromIndex == toIndex) return items
        val working = items.toMutableList()
        working.add(toIndex, working.removeAt(fromIndex))
        return renumber(working)
    }

    /**
     * The order the cards are shown in: what is still to do first, in list order, then what
     * is finished, the earliest finished first. Worked out on reading rather than stored, so
     * items ticked off anywhere, or before this order existed, still land at the back.
     */
    fun queue(items: List<TodoItem>): List<TodoItem> {
        val (done, open) = items.partition { it.isDone }
        return open.sortedBy { it.position } +
            done.sortedWith(compareBy<TodoItem> { it.doneAt }.thenBy { it.position })
    }

    /** Closes gaps left by deleting or moving, keeping the current list order. */
    fun renumber(items: List<TodoItem>): List<TodoItem> =
        items.mapIndexed { index, item -> item.copy(position = index) }

    /**
     * Copies a previous day's items as a fresh list for [newDate].
     *
     * Everything recording history is dropped: the done and started instants, and the ids, so the
     * copies save as new rows rather than overwriting the day they came from. The old
     * note goes too — today's note is today's to write.
     */
    fun templateFrom(day: DayList, newDate: LocalDate): DayList =
        DayList(
            date = newDate,
            note = null,
            items = renumber(
                day.items.map { TodoItem(0L, it.position, it.text, icon = it.icon, description = it.description) },
            ),
        )
}
