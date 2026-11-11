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

/**
 * Version 8 → 9: free-text history notes get their own table. And "last watered", given
 * when a repeating plant was added, becomes a real watering for plants never watered since,
 * so it shows in the week strip and the history (new plants now log it on save). The
 * schedule is unchanged: it counts from that same day either way.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `note` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`text` TEXT NOT NULL, `date` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "INSERT INTO watering (plantId, date, wateredAt) " +
                "SELECT id, anchor, CAST(strftime('%s', anchor || ' 05:00:00') AS INTEGER) * 1000 FROM plant " +
                "WHERE everyDays IS NOT NULL AND id NOT IN (SELECT plantId FROM watering)",
        )
    }
}

/**
 * Version 9 → 10: each plant keeps a diary of photos with a short note, instead of one
 * photo that a new one replaced. The photo a plant already had becomes its first entry,
 * dated the day it was got (or added, for a wish), with no note.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `dex_photo` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`dexId` INTEGER NOT NULL, `file` TEXT NOT NULL, `note` TEXT NOT NULL, `takenOn` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`dexId`) REFERENCES `dex`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_dex_photo_dexId` ON `dex_photo` (`dexId`)")
        db.execSQL(
            "INSERT INTO dex_photo (dexId, file, note, takenOn, createdAt) " +
                "SELECT id, photo, '', COALESCE(acquired, date(createdAt / 1000, 'unixepoch')), createdAt " +
                "FROM dex WHERE photo IS NOT NULL",
        )
    }
}

/** Every migration, in order, for the app and its tests. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
