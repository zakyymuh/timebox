package dev.zakyymuh.habits

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.room.Room
import dev.zakyymuh.habits.data.HabitsDatabase
import dev.zakyymuh.habits.data.MIGRATION_1_2
import dev.zakyymuh.habits.data.MIGRATION_2_3
import dev.zakyymuh.habits.data.MIGRATION_3_4
import dev.zakyymuh.habits.data.TimeboxRepository
import dev.zakyymuh.habits.widget.TodayWidget
import java.time.Clock

/**
 * Hand-rolled, as in papacti. The widget and the morning receiver reach the repository
 * from a cold-started process, so this has to work without an Activity alive.
 */
object ServiceLocator {

    @Volatile
    private var repository: TimeboxRepository? = null

    fun repository(context: Context): TimeboxRepository =
        repository ?: synchronized(this) {
            repository ?: TimeboxRepository(
                // Never fallbackToDestructiveMigration: that would delete the user's lists.
                dao = Room.databaseBuilder(context.applicationContext, HabitsDatabase::class.java, "habits.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .dao(),
                clock = Clock.systemDefaultZone(),
                onChanged = { TodayWidget().updateAll(context.applicationContext) },
            ).also { repository = it }
        }
}
