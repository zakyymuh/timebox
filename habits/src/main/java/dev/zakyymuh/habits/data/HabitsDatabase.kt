package dev.zakyymuh.habits.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Today's lists, and nothing else. Version 1: this app started fresh when the list moved
 * out of papacti, so there is no older schema to migrate from. The tables are the same
 * as papacti's `day` and `todo_item` were.
 */
@Database(entities = [DayEntity::class, TodoItemEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class HabitsDatabase : RoomDatabase() {
    abstract fun dao(): TimeboxDao
}
