package dev.zakyymuh.timebox

import android.content.Context
import androidx.room.Room
import dev.zakyymuh.timebox.data.ALL_MIGRATIONS
import dev.zakyymuh.timebox.data.DexRepository
import dev.zakyymuh.timebox.data.PhotoStore
import dev.zakyymuh.timebox.data.SoilRepository
import dev.zakyymuh.timebox.data.PlantRepository
import dev.zakyymuh.timebox.data.TimeboxDatabase
import java.time.Clock

/**
 * Hand-rolled rather than Hilt: a few repositories and one database in a single-user app
 * does not earn an annotation processor and a graph.
 *
 * The notification receivers reach the repositories from a cold-started
 * process, so this has to work without an Activity alive.
 */
object ServiceLocator {

    @Volatile
    private var database: TimeboxDatabase? = null

    @Volatile
    private var plantRepository: PlantRepository? = null

    var clock: Clock = Clock.systemDefaultZone()
        private set

    fun plantRepository(context: Context): PlantRepository =
        plantRepository ?: synchronized(this) {
            plantRepository ?: PlantRepository(
                dao = database(context).plantDao(),
                clock = clock,
            ).also { plantRepository = it }
        }

    @Volatile
    private var dexRepository: DexRepository? = null

    @Volatile
    private var photoStore: PhotoStore? = null

    @Volatile
    private var soilRepository: SoilRepository? = null

    fun soilRepository(context: Context): SoilRepository =
        soilRepository ?: synchronized(this) {
            soilRepository ?: SoilRepository(database(context).airTestDao(), database(context).plantDao(), clock)
                .also { soilRepository = it }
        }

    fun photoStore(context: Context): PhotoStore =
        photoStore ?: synchronized(this) {
            photoStore ?: PhotoStore(context.applicationContext).also { photoStore = it }
        }

    fun dexRepository(context: Context): DexRepository =
        dexRepository ?: synchronized(this) {
            dexRepository ?: DexRepository(
                dao = database(context).dexDao(),
                clock = clock,
                photos = photoStore(context),
            ).also { dexRepository = it }
        }

    private fun database(context: Context): TimeboxDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                TimeboxDatabase::class.java,
                "timebox.db",
            )
                // Never fallbackToDestructiveMigration: that would delete the user's lists.
                .addMigrations(*ALL_MIGRATIONS)
                .build()
                .also { database = it }
        }
}
