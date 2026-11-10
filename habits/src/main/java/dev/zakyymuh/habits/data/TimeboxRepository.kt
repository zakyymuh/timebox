package dev.zakyymuh.habits.data

import dev.zakyymuh.timebox.core.DayList
import dev.zakyymuh.timebox.core.Items
import dev.zakyymuh.timebox.core.TodoItem
import dev.zakyymuh.timebox.core.lastdone.Event
import dev.zakyymuh.timebox.core.lastdone.LastDone
import dev.zakyymuh.timebox.core.lastdone.Mark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

/**
 * The only door to storage.
 *
 * [today] never emits null: a day with nothing written yet is an empty [DayList], which
 * is the state every single morning starts in. Making the caller handle a null as well
 * as an empty list would be two representations of the same thing.
 */
class TimeboxRepository(
    private val dao: TimeboxDao,
    private val clock: Clock,
    /**
     * Called after every write. The home-screen widget refreshes on change rather than
     * on a timer, and putting the hook here rather than at each call site means no
     * caller — screen, notification receiver, or anything added later — can forget it.
     */
    private val onChanged: suspend () -> Unit = {},
) {

    private fun today(): LocalDate = LocalDate.now(clock)

    /** Today, for callers that need to know which day is the editable one. */
    fun todayDate(): LocalDate = today()

    /**
     * Stepping through the record skips empty days: paging one calendar day at a time
     * through a week you did not use the app would be all blank pages.
     */
    suspend fun previousDayWithItems(from: LocalDate): LocalDate? =
        dao.previousDayWithItems(from)

    suspend fun nextDayWithItems(from: LocalDate): LocalDate? = dao.nextDayWithItems(from)

    fun todayFlow(): Flow<DayList> = dayFlow(today())

    fun dayFlow(date: LocalDate): Flow<DayList> =
        combine(dao.dayFlow(date), dao.itemsFlow(date)) { day, items ->
            DayList(date, day?.note, items.map(TodoItemEntity::toModel))
        }

    suspend fun todayNow(): DayList = dayNow(today())

    suspend fun dayNow(date: LocalDate): DayList =
        DayList(date, dao.day(date)?.note, dao.items(date).map(TodoItemEntity::toModel))

    suspend fun saveNote(text: String) {
        val trimmed = text.trim()
        dao.upsertDay(DayEntity(today(), trimmed.ifEmpty { null }))
        onChanged()
    }

    suspend fun addItem(text: String, icon: String? = null, description: String? = null) {
        if (text.isBlank()) return
        val date = today()
        val existing = dao.items(date)
        dao.upsertItems(
            listOf(
                TodoItemEntity(
                    0L, date, existing.size, text.trim(), null,
                    icon = icon?.trim()?.ifEmpty { null },
                    description = description?.trim()?.ifEmpty { null },
                ),
            ),
        )
        onChanged()
    }

    /** "Working on it". Returns false if refused; see [TimeboxDao.startWorking]. */
    suspend fun startWorking(itemId: Long): Boolean {
        val changed = dao.startWorking(itemId, clock.instant()) > 0
        if (changed) onChanged()
        return changed
    }

    suspend fun toggle(itemId: Long) {
        val date = today()
        val updated = Items.toggle(dayNow(date), itemId, clock.instant())
        dao.upsertItems(updated.items.map { it.toEntity(date) })
        onChanged()
    }

    /**
     * Sets an item done or not, doing nothing if it already is. Returns whether it changed,
     * so a one-tap action from the dashboard can tell a real tick from a repeated tap.
     */
    suspend fun setDone(itemId: Long, done: Boolean): Boolean {
        val changed = if (done) dao.markDone(itemId, clock.instant()) else dao.markUndone(itemId)
        if (changed > 0) onChanged()
        return changed > 0
    }

    suspend fun rename(itemId: Long, text: String) {
        if (text.isBlank()) return
        val date = today()
        val updated = Items.rename(dayNow(date), itemId, text)
        dao.upsertItems(updated.items.map { it.toEntity(date) })
        onChanged()
    }

    suspend fun remove(itemId: Long) {
        val date = today()
        dao.deleteItem(itemId)
        // Renumber what is left so positions stay contiguous.
        dao.upsertItems(Items.renumber(dao.items(date).map(TodoItemEntity::toModel)).map { it.toEntity(date) })
        onChanged()
    }

    suspend fun move(fromIndex: Int, toIndex: Int) {
        val date = today()
        val items = dao.items(date).map(TodoItemEntity::toModel)
        if (fromIndex !in items.indices || toIndex !in items.indices) return
        dao.upsertItems(Items.reorder(items, fromIndex, toIndex).map { it.toEntity(date) })
        onChanged()
    }

    /** Copies a previous day's items into today, without its history. */
    suspend fun useTemplate(from: LocalDate) {
        val date = today()
        val source = dayNow(from)
        val fresh = Items.templateFrom(source, date)
        dao.deleteItemsOf(date)
        dao.upsertItems(fresh.items.map { it.toEntity(date) })
        onChanged()
    }

    /** Recent days that had items, newest first, for the one-tap morning path. */
    suspend fun recentDays(limit: Int): List<DayList> =
        dao.recentDatesWithItems(today(), limit).map { dayNow(it) }

    // ---- last time I did … -----------------------------------------------------
    // No onChanged here: the widget shows today's list, which marks never change.

    /** Every logged thing, the longest ago first. */
    fun eventsFlow(): Flow<List<Event>> =
        dao.marksFlow().map { rows -> LastDone.summarise(rows.map { Mark(it.name, it.at) }, clock.zone) }

    /** "I did this just now." A new name starts a new event. */
    suspend fun markDone(name: String) {
        if (name.isBlank()) return
        dao.insertMark(MarkEntity(name = name.trim(), at = clock.instant()))
    }

    /**
     * Takes back the most recent tap for [key], for a mistaken one. Undoing the only
     * tap removes the event. Matched in Kotlin rather than SQL, whose lower() is ASCII-only.
     */
    suspend fun undoLastMark(key: String) {
        dao.marks()
            .filter { LastDone.key(it.name) == key }
            .maxWithOrNull(compareBy<MarkEntity> { it.at }.thenBy { it.id })
            ?.let { dao.deleteMark(it.id) }
    }
}

private fun TodoItemEntity.toModel() = TodoItem(id, position, text, doneAt, icon, description, startedAt)

private fun TodoItem.toEntity(date: LocalDate) =
    TodoItemEntity(id, date, position, text, doneAt, icon, description, startedAt)
