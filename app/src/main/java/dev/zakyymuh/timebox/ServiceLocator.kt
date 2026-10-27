package dev.zakyymuh.timebox

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.room.Room
import dev.zakyymuh.timebox.data.TimeboxDatabase
import dev.zakyymuh.timebox.data.TimeboxRepository
import dev.zakyymuh.timebox.widget.TodayWidget
import java.time.Clock

/**
 * Hand-rolled rather than Hilt: one repository and one database in a single-user app
 * does not earn an annotation processor and a graph.
 *
 * The widget and the notification receiver both reach the repository from a cold-started
 * process, so this has to work without an Activity alive.
 */
object ServiceLocator {

    @Volatile
    private var database: TimeboxDatabase? = null

    @Volatile
    private var repository: TimeboxRepository? = null

    var clock: Clock = Clock.systemDefaultZone()
        private set

    fun repository(context: Context): TimeboxRepository =
        repository ?: synchronized(this) {
            repository ?: TimeboxRepository(
                dao = database(context).dao(),
                clock = clock,
                onChanged = { TodayWidget().updateAll(context.applicationContext) },
            ).also { repository = it }
        }

    private fun database(context: Context): TimeboxDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                TimeboxDatabase::class.java,
                "timebox.db",
            ).build().also { database = it }
        }
}
