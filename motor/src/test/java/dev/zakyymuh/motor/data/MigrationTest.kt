package dev.zakyymuh.motor.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

/**
 * The phone already holds a version 1 database with real services in it. This builds
 * that file by hand, exactly as version 1 created it, and opens it with today's code.
 * Room validates the migrated schema against the entities on open and throws if they
 * differ, so a passing test also proves the migration SQL matches what Room expects.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MigrationTest {

    private val name = "motor-migration-test.db"
    private val context = RuntimeEnvironment.getApplication()
    private var db: MotorDatabase? = null

    @Before
    fun clean() {
        context.deleteDatabase(name)
    }

    @After
    fun close() {
        db?.close()
        context.deleteDatabase(name)
    }

    private fun createVersion1(services: List<String>) {
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { v1 ->
            v1.execSQL(
                "CREATE TABLE IF NOT EXISTS `service` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, " +
                    "`place` TEXT NOT NULL, `price` INTEGER, `work` TEXT NOT NULL, `odometer` INTEGER, `createdAt` INTEGER NOT NULL)",
            )
            services.forEachIndexed { i, place ->
                v1.execSQL(
                    "INSERT INTO service (date, place, price, work, odometer, createdAt) " +
                        "VALUES ('2026-0${i + 1}-15', '$place', 150000, 'oil', ${12_000 + i * 1_000}, ${1_700_000_000_000 + i})",
                )
            }
            v1.version = 1
        }
    }

    private fun open(): MotorDatabase =
        Room.databaseBuilder(context, MotorDatabase::class.java, name)
            .addMigrations(*MIGRATIONS)
            .allowMainThreadQueries()
            .build()
            .also { db = it }

    @Test
    fun `version 1 services all move to one new motorcycle`() = runTest {
        createVersion1(listOf("AHASS", "Bengkel Jaya"))
        val current = open()

        val vehicles = current.vehicleDao().vehiclesFlow().first()
        assertEquals(listOf("My motorcycle"), vehicles.map { it.name })
        val services = current.serviceDao().recordsFlow(vehicles.single().id).first()
        assertEquals(listOf("Bengkel Jaya", "AHASS"), services.map { it.place })
        assertEquals(13_000, services.first().odometer)
    }

    @Test
    fun `the upgrade adds an empty dates table that works`() = runTest {
        createVersion1(listOf("AHASS"))
        val current = open()
        assertTrue(current.expiryDao().all().isEmpty())
        val vehicleId = current.vehicleDao().vehiclesFlow().first().single().id
        current.expiryDao().upsert(
            ExpiryEntity(vehicleId = vehicleId, name = "STNK tax", date = LocalDate.of(2027, 3, 12), renewMonths = 12, createdAt = Instant.EPOCH),
        )
        assertEquals(listOf("STNK tax"), current.expiryDao().all().map { it.name })
    }

    @Test
    fun `the upgrade adds an empty chores table that works`() = runTest {
        createVersion1(emptyList())
        val current = open()
        assertTrue(current.markDao().marks().isEmpty())
        current.markDao().insert(MarkEntity(name = "SIM for car", at = Instant.EPOCH, vehicleId = null))
        assertEquals(listOf("SIM for car"), current.markDao().marks().map { it.name })
    }

    @Test
    fun `an empty version 1 database gets no made-up vehicle`() = runTest {
        createVersion1(emptyList())
        val current = open()
        assertTrue(current.vehicleDao().vehiclesFlow().first().isEmpty())
    }
}
