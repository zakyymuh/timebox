package dev.zakyymuh.timebox.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [DayEntity::class, TodoItemEntity::class, PlantEntity::class, WateringEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class TimeboxDatabase : RoomDatabase() {
    abstract fun dao(): TimeboxDao

    abstract fun plantDao(): PlantDao
}
