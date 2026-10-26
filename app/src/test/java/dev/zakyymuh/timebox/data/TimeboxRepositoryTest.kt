package dev.zakyymuh.timebox.data

import androidx.room.Room
import dev.zakyymuh.timebox.core.DaySummaries
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TimeboxRepositoryTest {

    private val zone = ZoneId.of("Asia/Jakarta")

    // 09:14 in Jakarta on 2026-10-27.
    private val now = Instant.parse("2026-10-27T02:14:00Z")
    private val today = LocalDate.of(2026, 10, 27)
    private val yesterday = LocalDate.of(2026, 10, 26)

    private lateinit var db: TimeboxDatabase
    private lateinit var repo: TimeboxRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            TimeboxDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = TimeboxRepository(db.dao(), Clock.fixed(now, zone))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `a day with nothing written is an empty list, not null`() = runTest {
        // The state every morning starts in.
        val day = repo.todayNow()
        assertEquals(today, day.date)
        assertNull(day.note)
        assertTrue(day.items.isEmpty())
    }

    @Test
    fun `saveNote then todayNow returns it, trimmed`() = runTest {
        repo.saveNote("  Ship the auth refactor  ")
        assertEquals("Ship the auth refactor", repo.todayNow().note)
    }

    @Test
    fun `saving a blank note clears it rather than storing whitespace`() = runTest {
        repo.saveNote("something")
        repo.saveNote("   ")
        assertNull(repo.todayNow().note)
    }

    @Test
    fun `addItem appends in order and assigns contiguous positions`() = runTest {
        repo.addItem("Workout")
        repo.addItem("Thesis chapter 3")
        repo.addItem("Reply to Rina")
        val items = repo.todayNow().items
        assertEquals(listOf("Workout", "Thesis chapter 3", "Reply to Rina"), items.map { it.text })
        assertEquals(listOf(0, 1, 2), items.map { it.position })
    }

    @Test
    fun `addItem ignores a blank text`() = runTest {
        repo.addItem("   ")
        assertTrue(repo.todayNow().items.isEmpty())
    }

    @Test
    fun `toggle marks an item done at the clock's instant`() = runTest {
        repo.addItem("Workout")
        val id = repo.todayNow().items.single().id
        repo.toggle(id)
        val item = repo.todayNow().items.single()
        assertTrue(item.isDone)
        assertEquals(now, item.doneAt)
    }

    @Test
    fun `toggle twice un-ticks the item`() = runTest {
        repo.addItem("Workout")
        val id = repo.todayNow().items.single().id
        repo.toggle(id)
        repo.toggle(id)
        assertFalse(repo.todayNow().items.single().isDone)
    }

    @Test
    fun `rename replaces the text and keeps the position`() = runTest {
        repo.addItem("Workout")
        repo.addItem("Thesis")
        val id = repo.todayNow().items.first().id
        repo.rename(id, "Long run")
        val items = repo.todayNow().items
        assertEquals(listOf("Long run", "Thesis"), items.map { it.text })
        assertEquals(listOf(0, 1), items.map { it.position })
    }

    @Test
    fun `remove deletes the item and closes the position gap`() = runTest {
        repo.addItem("a"); repo.addItem("b"); repo.addItem("c")
        val middle = repo.todayNow().items[1].id
        repo.remove(middle)
        val items = repo.todayNow().items
        assertEquals(listOf("a", "c"), items.map { it.text })
        assertEquals(listOf(0, 1), items.map { it.position })
    }

    @Test
    fun `move reorders and persists the new order`() = runTest {
        repo.addItem("a"); repo.addItem("b"); repo.addItem("c")
        repo.move(0, 2)
        assertEquals(listOf("b", "c", "a"), repo.todayNow().items.map { it.text })
    }

    @Test
    fun `move with an out-of-range index does nothing`() = runTest {
        repo.addItem("a"); repo.addItem("b")
        repo.move(0, 5)
        assertEquals(listOf("a", "b"), repo.todayNow().items.map { it.text })
    }

    @Test
    fun `useTemplate copies a previous day's items without its history`() = runTest {
        // Write yesterday directly, with one item already ticked off.
        db.dao().upsertItems(
            listOf(
                TodoItemEntity(0L, yesterday, 0, "Workout", now.minusSeconds(86_400)),
                TodoItemEntity(0L, yesterday, 1, "Thesis", null),
            ),
        )

        repo.useTemplate(yesterday)

        val items = repo.todayNow().items
        assertEquals(listOf("Workout", "Thesis"), items.map { it.text })
        assertTrue("a copied list must start fresh", items.none { it.isDone })
        assertEquals(listOf(0, 1), items.map { it.position })
    }

    @Test
    fun `useTemplate replaces today's items rather than appending to them`() = runTest {
        repo.addItem("leftover")
        db.dao().upsertItems(listOf(TodoItemEntity(0L, yesterday, 0, "Workout", null)))
        repo.useTemplate(yesterday)
        assertEquals(listOf("Workout"), repo.todayNow().items.map { it.text })
    }

    @Test
    fun `recentDays returns earlier days with items, newest first, excluding today`() = runTest {
        db.dao().upsertItems(
            listOf(
                TodoItemEntity(0L, yesterday, 0, "y", null),
                TodoItemEntity(0L, LocalDate.of(2026, 10, 24), 0, "older", null),
            ),
        )
        repo.addItem("today's thing")

        val recent = repo.recentDays(5)
        assertEquals(listOf(yesterday, LocalDate.of(2026, 10, 24)), recent.map { it.date })
    }

    @Test
    fun `items written yesterday stay on yesterday`() = runTest {
        db.dao().upsertItems(listOf(TodoItemEntity(0L, yesterday, 0, "y", null)))
        assertTrue(repo.todayNow().items.isEmpty())
        assertEquals(listOf("y"), repo.dayNow(yesterday).items.map { it.text })
    }

    @Test
    fun `the summary counts what is done out of the total`() = runTest {
        repo.addItem("a"); repo.addItem("b")
        repo.toggle(repo.todayNow().items.first().id)
        val s = DaySummaries.summarise(repo.todayNow())
        assertEquals(1, s.done)
        assertEquals(2, s.total)
    }
}
