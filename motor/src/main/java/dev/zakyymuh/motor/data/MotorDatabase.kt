package dev.zakyymuh.motor.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The motorcycle service log, and nothing else. Version 1: this app started fresh when
 * the service log moved out of papacti, so there is no older schema to migrate from.
 */
@Database(entities = [ServiceEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MotorDatabase : RoomDatabase() {
    abstract fun serviceDao(): ServiceDao
}
