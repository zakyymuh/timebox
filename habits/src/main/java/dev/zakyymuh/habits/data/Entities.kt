package dev.zakyymuh.habits.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/** One day's note. Rows exist only for days that have been written to. */
@Entity(tableName = "day")
data class DayEntity(
    @PrimaryKey val date: LocalDate,
    val note: String?,
)

/**
 * One thing to do, belonging to the day it was written on.
 *
 * Keyed to its day by date rather than by a running clock, so an item written at 23:50
 * stays on that day whatever time it is ticked off.
 */
@Entity(tableName = "todo_item", indices = [Index("date")])
data class TodoItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val position: Int,
    val text: String,
    val doneAt: Instant?,
)

/**
 * One time something was done — "laundry, at this moment". Every tap is kept, not just
 * the latest, so the app can say how often it usually happens.
 */
@Entity(tableName = "mark", indices = [Index("name")])
data class MarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val at: Instant,
)
