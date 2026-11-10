package dev.zakyymuh.habits.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TimeboxDao {

    @Query("SELECT * FROM day WHERE date = :date")
    fun dayFlow(date: LocalDate): Flow<DayEntity?>

    @Query("SELECT * FROM day WHERE date = :date")
    suspend fun day(date: LocalDate): DayEntity?

    @Upsert
    suspend fun upsertDay(day: DayEntity)

    @Query("SELECT * FROM todo_item WHERE date = :date ORDER BY position ASC")
    fun itemsFlow(date: LocalDate): Flow<List<TodoItemEntity>>

    @Query("SELECT * FROM todo_item WHERE date = :date ORDER BY position ASC")
    suspend fun items(date: LocalDate): List<TodoItemEntity>

    @Upsert
    suspend fun upsertItems(items: List<TodoItemEntity>)

    /** Atomic, so two quick taps can't tick and then untick. Returns rows changed. */
    @Query("UPDATE todo_item SET doneAt = :at WHERE id = :id AND doneAt IS NULL")
    suspend fun markDone(id: Long, at: java.time.Instant): Int

    /**
     * "Working on it". Atomic, and refused if the item is finished, already started, or
     * another item on its day is still in progress: one card holds the screen at a time.
     */
    @Query(
        """
        UPDATE todo_item SET startedAt = :at
        WHERE id = :id AND doneAt IS NULL AND startedAt IS NULL
          AND NOT EXISTS (
            SELECT 1 FROM todo_item AS other
            WHERE other.date = todo_item.date AND other.startedAt IS NOT NULL AND other.doneAt IS NULL
          )
        """,
    )
    suspend fun startWorking(id: Long, at: java.time.Instant): Int

    @Query("SELECT * FROM todo_item WHERE id = :id")
    suspend fun item(id: Long): TodoItemEntity?

    @Query("UPDATE todo_item SET doneAt = NULL WHERE id = :id AND doneAt IS NOT NULL")
    suspend fun markUndone(id: Long): Int

    @Query("DELETE FROM todo_item WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM todo_item WHERE date = :date")
    suspend fun deleteItemsOf(date: LocalDate)

    /** The nearest earlier day that has anything on it, for stepping back through the record. */
    @Query("SELECT MAX(date) FROM todo_item WHERE date < :date")
    suspend fun previousDayWithItems(date: LocalDate): LocalDate?

    /** The nearest later day that has anything on it. */
    @Query("SELECT MIN(date) FROM todo_item WHERE date > :date")
    suspend fun nextDayWithItems(date: LocalDate): LocalDate?

    /** Days before [before] that actually have items, newest first — the templates. */
    @Query(
        """
        SELECT DISTINCT date FROM todo_item
        WHERE date < :before
        ORDER BY date DESC
        LIMIT :limit
        """,
    )
    suspend fun recentDatesWithItems(before: LocalDate, limit: Int): List<LocalDate>

    // ---- last time I did … -----------------------------------------------------

    @Query("SELECT * FROM mark")
    fun marksFlow(): Flow<List<MarkEntity>>

    @Query("SELECT * FROM mark")
    suspend fun marks(): List<MarkEntity>

    @androidx.room.Insert
    suspend fun insertMark(mark: MarkEntity)

    @Query("DELETE FROM mark WHERE id = :id")
    suspend fun deleteMark(id: Long)
}
