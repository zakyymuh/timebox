package dev.zakyymuh.motor

import android.content.Context
import androidx.room.Room
import dev.zakyymuh.motor.data.MotorDatabase
import dev.zakyymuh.motor.data.ServiceRepository
import java.time.Clock

/** Hand-rolled, as in papacti: one database and one repository need no DI graph. */
object ServiceLocator {

    @Volatile
    private var repository: ServiceRepository? = null

    fun serviceRepository(context: Context): ServiceRepository =
        repository ?: synchronized(this) {
            repository ?: ServiceRepository(
                // Never fallbackToDestructiveMigration: that would delete the service log.
                Room.databaseBuilder(context.applicationContext, MotorDatabase::class.java, "motor.db")
                    .build()
                    .serviceDao(),
                Clock.systemDefaultZone(),
            ).also { repository = it }
        }
}
