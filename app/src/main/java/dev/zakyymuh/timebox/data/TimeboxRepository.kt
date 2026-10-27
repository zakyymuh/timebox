package dev.zakyymuh.timebox.data

import dev.zakyymuh.timebox.core.DayList
import dev.zakyymuh.timebox.core.Items
import dev.zakyymuh.timebox.core.TodoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    suspend fun addItem(text: String) {
        if (text.isBlank()) return
        val date = today()
        val existing = dao.items(date)
        dao.upsertItems(
            listOf(TodoItemEntity(0L, date, existing.size, text.trim(), null)),
        )
        onChanged()
    }

    suspend fun toggle(itemId: Long) {
        val date = today()
        val updated = Items.toggle(dayNow(date), itemId, clock.instant())
        dao.upsertItems(updated.items.map { it.toEntity(date) })
        onChanged()
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
}

private fun TodoItemEntity.toModel() = TodoItem(id, position, text, doneAt)

private fun TodoItem.toEntity(date: LocalDate) =
    TodoItemEntity(id, date, position, text, doneAt)
