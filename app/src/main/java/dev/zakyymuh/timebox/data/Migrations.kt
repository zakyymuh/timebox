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

/** Version 2 → 3: "soil still moist" snoozes. One nullable column; every row is kept. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `plant` ADD COLUMN `snoozedUntil` TEXT")
    }
}

/** Version 3 → 4: the plant collection and wishlist. Only creates a table; nothing else is touched. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `dex` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`number` INTEGER NOT NULL, `name` TEXT NOT NULL, `species` TEXT NOT NULL, " +
                "`owned` INTEGER NOT NULL, `photo` TEXT, `acquired` TEXT, `note` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_dex_number` ON `dex` (`number`)")
    }
}

/** Every migration, in order, for the app and its tests. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
