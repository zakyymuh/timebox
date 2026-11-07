package dev.zakyymuh.timebox.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 1 → 2: adds plants and their waterings. Only creates tables, so every daily
 * list already on the phone is untouched. The SQL must match what Room generates for
 * [PlantEntity] and [WateringEntity] exactly; MigrationTest fails if it drifts.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `plant` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `colour` TEXT NOT NULL, `everyDays` INTEGER, `onceDate` TEXT, " +
                "`anchor` TEXT NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `watering` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`plantId` INTEGER NOT NULL, `date` TEXT NOT NULL, `wateredAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`plantId`) REFERENCES `plant`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_watering_plantId` ON `watering` (`plantId`)")
    }
}
