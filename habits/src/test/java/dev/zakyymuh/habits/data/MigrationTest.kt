package dev.zakyymuh.habits.data

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

    private val name = "habits-migration-test.db"
    private val context = RuntimeEnvironment.getApplication()
    private val day = LocalDate.of(2026, 11, 7)
    private var db: HabitsDatabase? = null

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

    @Test
    fun `a version 1 database upgrades with its lists kept and no marks`() = runTest {
        createVersion1()
        val current = Room.databaseBuilder(context, HabitsDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
            .also { db = it }

        assertEquals(listOf("cuci piring", "repotting", "beberes"), current.dao().items(day).map { it.text })
        assertEquals("tidy up", current.dao().day(day)?.note)
        assertTrue(current.dao().marksFlow().first().isEmpty())
    }

    @Test
    fun `a version 1 database upgrades to 3 with every card plain and not in progress`() = runTest {
        createVersion1()
        val current = Room.databaseBuilder(context, HabitsDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
            .also { db = it }

        val items = current.dao().items(day)
        assertEquals(listOf("cuci piring", "repotting", "beberes"), items.map { it.text })
        assertTrue(items.all { it.icon == null && it.description == null && it.startedAt == null })
    }
}
