package dev.zakyymuh.motor

import android.content.Context
import androidx.room.Room
import dev.zakyymuh.motor.data.ExpiryRepository
import dev.zakyymuh.motor.data.MIGRATIONS
import dev.zakyymuh.motor.data.MotorDatabase
import dev.zakyymuh.motor.data.PhotoStore
import dev.zakyymuh.motor.data.ReminderRepository
import dev.zakyymuh.motor.data.ServiceRepository
import dev.zakyymuh.motor.data.VehicleRepository
import java.time.Clock

/** Hand-rolled, as in papacti: one database and a few repositories need no DI graph. */
object ServiceLocator {

    private val clock: Clock = Clock.systemDefaultZone()

    @Volatile
    private var database: MotorDatabase? = null

    private fun db(context: Context): MotorDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(context.applicationContext, MotorDatabase::class.java, "motor.db")
                // Never fallbackToDestructiveMigration: that would delete the service log.
                .addMigrations(*MIGRATIONS)
                .build()
                .also { database = it }
        }

    fun serviceRepository(context: Context) = ServiceRepository(db(context).serviceDao(), clock)

    fun vehicleRepository(context: Context) = VehicleRepository(db(context).vehicleDao(), clock)

    fun expiryRepository(context: Context) = ExpiryRepository(db(context).expiryDao(), clock)

    fun reminderRepository(context: Context) =
        ReminderRepository(db(context).reminderDao(), expiryRepository(context), vehicleRepository(context), clock)

    fun photoStore(context: Context) = PhotoStore(context.applicationContext)
}
