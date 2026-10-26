package dev.zakyymuh.timebox.data

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

    @Query("DELETE FROM todo_item WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM todo_item WHERE date = :date")
    suspend fun deleteItemsOf(date: LocalDate)

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
}
