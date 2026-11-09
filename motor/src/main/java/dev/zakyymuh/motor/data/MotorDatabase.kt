package dev.zakyymuh.motor.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Your vehicles and their service logs.
 *
 * Version 1 started fresh when the service log moved out of papacti: one `service` table.
 * Version 2 adds `vehicle` and ties every service to one. Version 3 adds `expiry`,
 * version 4 `reminder_sent`, and version 5 `mark`.
 */
@Database(
    entities = [VehicleEntity::class, ServiceEntity::class, ExpiryEntity::class, ReminderSentEntity::class, MarkEntity::class],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MotorDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun serviceDao(): ServiceDao
    abstract fun expiryDao(): ExpiryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun markDao(): MarkDao
}

/** Every migration, oldest first. Never fallbackToDestructiveMigration: that would delete the log. */
val MIGRATIONS: Array<Migration> get() = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

/**
 * Adds `vehicle`, and a `vehicleId` on every service. Version 1 knew one motorcycle
 * only, so if there are services they all go to a new "My motorcycle"; rename it after.
 *
 * SQLite cannot add a foreign-key column in place, so `service` is rebuilt and copied.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `vehicle` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`type` TEXT NOT NULL, `name` TEXT NOT NULL, `plate` TEXT NOT NULL, `photo` TEXT, `createdAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "INSERT INTO vehicle (type, name, plate, photo, createdAt) " +
                "SELECT 'MOTORCYCLE', 'My motorcycle', '', NULL, (SELECT MIN(createdAt) FROM service) " +
                "WHERE EXISTS (SELECT 1 FROM service)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `service_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`vehicleId` INTEGER NOT NULL, `date` TEXT NOT NULL, `place` TEXT NOT NULL, `price` INTEGER, " +
                "`work` TEXT NOT NULL, `odometer` INTEGER, `createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`vehicleId`) REFERENCES `vehicle`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "INSERT INTO service_new (id, vehicleId, date, place, price, work, odometer, createdAt) " +
                "SELECT id, (SELECT MIN(id) FROM vehicle), date, place, price, work, odometer, createdAt FROM service",
        )
        db.execSQL("DROP TABLE service")
        db.execSQL("ALTER TABLE service_new RENAME TO service")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_vehicleId` ON `service` (`vehicleId`)")
    }
}

/** Adds `expiry`. Nothing existing is touched. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `expiry` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` INTEGER, " +
                "`name` TEXT NOT NULL, `date` TEXT NOT NULL, `renewMonths` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`vehicleId`) REFERENCES `vehicle`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expiry_vehicleId` ON `expiry` (`vehicleId`)")
    }
}

/** Adds `reminder_sent`. Nothing existing is touched. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reminder_sent` (`expiryId` INTEGER NOT NULL, `date` TEXT NOT NULL, `tag` TEXT NOT NULL, " +
                "PRIMARY KEY(`expiryId`, `date`, `tag`), " +
                "FOREIGN KEY(`expiryId`) REFERENCES `expiry`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
    }
}

/** Adds `mark`, for "when did I last …". Nothing existing is touched. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `mark` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                "`at` INTEGER NOT NULL, `vehicleId` INTEGER, " +
                "FOREIGN KEY(`vehicleId`) REFERENCES `vehicle`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_mark_vehicleId` ON `mark` (`vehicleId`)")
    }
}
