package dev.zakyymuh.timebox.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [DayEntity::class, TodoItemEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class TimeboxDatabase : RoomDatabase() {
    abstract fun dao(): TimeboxDao
}
