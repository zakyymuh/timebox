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

/** Version 4 → 5: a price (or price range) on wishlist plants. Two nullable columns; every row kept. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `dex` ADD COLUMN `priceLow` INTEGER")
        db.execSQL("ALTER TABLE `dex` ADD COLUMN `priceHigh` INTEGER")
    }
}

/** Version 5 → 6: the motorcycle service log. Only creates a table. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `service` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` TEXT NOT NULL, `place` TEXT NOT NULL, `price` INTEGER, `work` TEXT NOT NULL, " +
                "`odometer` INTEGER, `createdAt` INTEGER NOT NULL)",
        )
    }
}

/** Version 6 → 7: soil air-space tests. Only creates a table. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `air_test` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` TEXT NOT NULL, `mix` TEXT NOT NULL, `potMl` INTEGER NOT NULL, `drainedMl` INTEGER NOT NULL, " +
                "`pouredMl` INTEGER, `createdAt` INTEGER NOT NULL)",
        )
    }
}

/**
 * Version 7 → 8: today's lists and the motorcycle service log moved to their own apps.
 * Their tables go; every plant, watering, collection entry and soil test stays. The
 * owner chose to re-enter the lists and services in the new apps by hand.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `todo_item`")
        db.execSQL("DROP TABLE IF EXISTS `day`")
        db.execSQL("DROP TABLE IF EXISTS `service`")
    }
}

/** Every migration, in order, for the app and its tests. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
