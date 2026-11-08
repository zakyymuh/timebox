package dev.zakyymuh.timebox.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * The phone already holds a version 1 database with real lists in it. This builds that
 * file by hand, exactly as version 1 created it, and opens it with today's code. Room
 * validates the migrated schema against the entities on open and throws if they differ,
 * so a passing test also proves the migration SQL matches what Room expects.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MigrationTest {

    private val name = "migration-test.db"
    private val context = RuntimeEnvironment.getApplication()
    private val day = LocalDate.of(2026, 11, 7)
    private var db: TimeboxDatabase? = null

    @Before
    fun clean() {
        context.deleteDatabase(name)
    }

    @After
    fun close() {
        db?.close()
        context.deleteDatabase(name)
    }

    private fun createVersion1() {
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { v1 ->
            v1.execSQL("CREATE TABLE IF NOT EXISTS `day` (`date` TEXT NOT NULL, `note` TEXT, PRIMARY KEY(`date`))")
            v1.execSQL("CREATE TABLE IF NOT EXISTS `todo_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `position` INTEGER NOT NULL, `text` TEXT NOT NULL, `doneAt` INTEGER)")
            v1.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_item_date` ON `todo_item` (`date`)")
            v1.execSQL("INSERT INTO day (date, note) VALUES ('$day', 'tidy up')")
            listOf("cuci piring", "repotting", "beberes").forEachIndexed { i, text ->
                v1.execSQL("INSERT INTO todo_item (date, position, text, doneAt) VALUES ('$day', $i, '$text', NULL)")
            }
            v1.version = 1
        }
    }

    private fun openCurrent(): TimeboxDatabase =
        Room.databaseBuilder(context, TimeboxDatabase::class.java, name)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
            .also { db = it }

    @Test
    fun `a version 1 database with lists upgrades with every item intact`() = runTest {
        createVersion1()
        val current = openCurrent()

        assertEquals(listOf("cuci piring", "repotting", "beberes"), current.dao().items(day).map { it.text })
        assertEquals("tidy up", current.dao().day(day)?.note)
        assertTrue(current.plantDao().plants().isEmpty())
    }

    @Test
    fun `deleting a plant deletes its waterings`() = runTest {
        createVersion1()
        val plants = openCurrent().plantDao()
        val id = plants.upsertPlant(
            PlantEntity(name = "Basil", colour = "DUSK", everyDays = 3, onceDate = null, anchor = day),
        )
        plants.insertWatering(WateringEntity(plantId = id, date = day, wateredAt = Instant.EPOCH))

        plants.deletePlant(id)

        assertTrue(plants.waterings().isEmpty())
    }

    @Test
    fun `a version 2 database with a plant upgrades intact`() = runTest {
        createVersion1()
        val file = context.getDatabasePath(name)
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { v2 ->
            v2.execSQL("CREATE TABLE IF NOT EXISTS `plant` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `colour` TEXT NOT NULL, `everyDays` INTEGER, `onceDate` TEXT, `anchor` TEXT NOT NULL)")
            v2.execSQL("CREATE TABLE IF NOT EXISTS `watering` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `plantId` INTEGER NOT NULL, `date` TEXT NOT NULL, `wateredAt` INTEGER NOT NULL, FOREIGN KEY(`plantId`) REFERENCES `plant`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            v2.execSQL("CREATE INDEX IF NOT EXISTS `index_watering_plantId` ON `watering` (`plantId`)")
            v2.execSQL("INSERT INTO plant (id, name, colour, everyDays, onceDate, anchor) VALUES (1, 'Greenovia', 'MOSS', 4, NULL, '$day')")
            v2.execSQL("INSERT INTO watering (plantId, date, wateredAt) VALUES (1, '$day', 0)")
            v2.version = 2
        }
        val current = Room.databaseBuilder(context, TimeboxDatabase::class.java, name)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
            .also { db = it }

        val plant = current.plantDao().plants().single()
        assertEquals("Greenovia", plant.name)
        assertEquals(null, plant.snoozedUntil)
        assertEquals(1, current.plantDao().waterings().size)
        assertEquals(3, current.dao().items(day).size)
    }

    @Test
    fun `a version 3 database upgrades with lists and plants intact and an empty collection`() = runTest {
        createVersion1()
        val file = context.getDatabasePath(name)
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { v3 ->
            v3.execSQL("CREATE TABLE IF NOT EXISTS `plant` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `colour` TEXT NOT NULL, `everyDays` INTEGER, `onceDate` TEXT, `anchor` TEXT NOT NULL, `snoozedUntil` TEXT)")
            v3.execSQL("CREATE TABLE IF NOT EXISTS `watering` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `plantId` INTEGER NOT NULL, `date` TEXT NOT NULL, `wateredAt` INTEGER NOT NULL, FOREIGN KEY(`plantId`) REFERENCES `plant`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            v3.execSQL("CREATE INDEX IF NOT EXISTS `index_watering_plantId` ON `watering` (`plantId`)")
            v3.execSQL("INSERT INTO plant (id, name, colour, everyDays, onceDate, anchor, snoozedUntil) VALUES (1, 'Greenovia', 'MOSS', 4, NULL, '$day', NULL)")
            v3.version = 3
        }
        val current = openCurrent()

        assertEquals("Greenovia", current.plantDao().plants().single().name)
        assertEquals(3, current.dao().items(day).size)
        assertTrue(current.dexDao().entries().isEmpty())
        val id = current.dexDao().insert(
            DexEntity(number = 1, name = "Astrophytum", species = "", owned = true, photo = null, acquired = day, note = "", createdAt = Instant.EPOCH),
        )
        assertEquals("Astrophytum", current.dexDao().entry(id)?.name)
    }

    @Test
    fun `a version 4 collection upgrades with its plants intact and no price`() = runTest {
        createVersion1()
        val file = context.getDatabasePath(name)
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { v4 ->
            v4.execSQL("CREATE TABLE IF NOT EXISTS `plant` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `colour` TEXT NOT NULL, `everyDays` INTEGER, `onceDate` TEXT, `anchor` TEXT NOT NULL, `snoozedUntil` TEXT)")
            v4.execSQL("CREATE TABLE IF NOT EXISTS `watering` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `plantId` INTEGER NOT NULL, `date` TEXT NOT NULL, `wateredAt` INTEGER NOT NULL, FOREIGN KEY(`plantId`) REFERENCES `plant`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            v4.execSQL("CREATE INDEX IF NOT EXISTS `index_watering_plantId` ON `watering` (`plantId`)")
            v4.execSQL("CREATE TABLE IF NOT EXISTS `dex` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `number` INTEGER NOT NULL, `name` TEXT NOT NULL, `species` TEXT NOT NULL, `owned` INTEGER NOT NULL, `photo` TEXT, `acquired` TEXT, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
            v4.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_dex_number` ON `dex` (`number`)")
            v4.execSQL("INSERT INTO dex (number, name, species, owned, photo, acquired, note, createdAt) VALUES (1, 'Kikko', '', 0, NULL, NULL, 'Shopee', 0)")
            v4.version = 4
        }
        val current = openCurrent()

        val wish = current.dexDao().entries().single()
        assertEquals("Kikko", wish.name)
        assertEquals("Shopee", wish.note)
        assertEquals(null, wish.priceLow)
        assertEquals(3, current.dao().items(day).size)
    }

    @Test
    fun `the upgrade to version 6 adds an empty service log that works`() = runTest {
        createVersion1()
        val current = openCurrent()
        val service = current.serviceDao()
        val id = service.upsert(
            ServiceEntity(date = day, place = "AHASS", price = 150_000, work = "oil", odometer = 12_000, createdAt = Instant.EPOCH),
        )
        assertEquals("AHASS", service.record(id)?.place)
        assertEquals(3, current.dao().items(day).size)
    }

    @Test
    fun `the upgrade to version 7 adds an empty soil test table that works`() = runTest {
        createVersion1()
        val current = openCurrent()
        val id = current.airTestDao().insert(
            AirTestEntity(date = day, mix = "Mix A", potMl = 500, drainedMl = 110, pouredMl = null, createdAt = Instant.EPOCH),
        )
        assertEquals(1, current.airTestDao().testsFlow().first().size)
        assertEquals(Instant.EPOCH, current.airTestDao().lastTestAt())
        assertTrue(id > 0)
    }

    /**
     * The database on the phone when the apps were split: version 7, with lists, a
     * service record, a plant and its watering, a collection entry and a soil test.
     * Built from the v1 file by running the real 1→7 migrations, so it is exactly what
     * an upgrade from any older install would have produced.
     */
    private fun createVersion7() {
        createVersion1()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                    override fun onCreate(db: SupportSQLiteDatabase) = error("the v1 file must already exist")
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        listOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                            .forEach { it.migrate(db) }
                    }
                })
                .build(),
        )
        helper.writableDatabase.apply {
            execSQL("INSERT INTO service (date, place, price, work, odometer, createdAt) VALUES ('$day', 'AHASS', 150000, 'oil', 12000, 0)")
            execSQL("INSERT INTO plant (id, name, colour, everyDays, onceDate, anchor, snoozedUntil) VALUES (1, 'Greenovia', 'MOSS', 4, NULL, '$day', NULL)")
            execSQL("INSERT INTO watering (plantId, date, wateredAt) VALUES (1, '$day', 0)")
            execSQL("INSERT INTO dex (number, name, species, owned, photo, acquired, note, createdAt, priceLow, priceHigh) VALUES (1, 'Astrophytum', '', 1, NULL, '$day', '', 0, NULL, NULL)")
            execSQL("INSERT INTO air_test (date, mix, potMl, drainedMl, pouredMl, createdAt) VALUES ('$day', 'Mix A', 500, 110, NULL, 0)")
        }
        helper.close()
    }

    private fun TimeboxDatabase.tables(): List<String> =
        openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }

    @Test
    fun `a version 7 database upgrades to 8 with plants intact and habit and service tables gone`() = runTest {
        createVersion7()
        val current = openCurrent()

        assertEquals("Greenovia", current.plantDao().plants().single().name)
        assertEquals(1, current.plantDao().waterings().size)
        assertEquals("Astrophytum", current.dexDao().entries().single().name)
        assertEquals(1, current.airTestDao().testsFlow().first().size)
        val tables = current.tables()
        assertFalse("day" in tables)
        assertFalse("todo_item" in tables)
        assertFalse("service" in tables)
    }
}
