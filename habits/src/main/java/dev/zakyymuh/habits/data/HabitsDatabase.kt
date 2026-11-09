package dev.zakyymuh.habits.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Today's lists, and the "last time I did …" marks.
 *
 * Version 1 started fresh when the list moved out of papacti; its tables are the same as
 * papacti's `day` and `todo_item` were. Version 2 adds `mark`.
 */
@Database(
    entities = [DayEntity::class, TodoItemEntity::class, MarkEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class HabitsDatabase : RoomDatabase() {
    abstract fun dao(): TimeboxDao
}

/** Adds the `mark` table. Nothing existing is touched, so the lists survive the upgrade. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `mark` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `at` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_mark_name` ON `mark` (`name`)")
    }
}
