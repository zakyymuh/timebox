package dev.zakyymuh.timebox.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PlantEntity::class, WateringEntity::class, DexEntity::class, AirTestEntity::class, NoteEntity::class, DexPhotoEntity::class, BreedingEntity::class],
    version = 12,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class TimeboxDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao

    abstract fun dexDao(): DexDao

    abstract fun airTestDao(): AirTestDao

    abstract fun noteDao(): NoteDao

    abstract fun breedingDao(): BreedingDao
}
