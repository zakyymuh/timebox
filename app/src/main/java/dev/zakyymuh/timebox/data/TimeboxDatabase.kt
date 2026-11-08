package dev.zakyymuh.timebox.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [DayEntity::class, TodoItemEntity::class, PlantEntity::class, WateringEntity::class, DexEntity::class],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class TimeboxDatabase : RoomDatabase() {
    abstract fun dao(): TimeboxDao

    abstract fun plantDao(): PlantDao

    abstract fun dexDao(): DexDao
}
