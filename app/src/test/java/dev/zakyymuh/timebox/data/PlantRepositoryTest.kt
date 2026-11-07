package dev.zakyymuh.timebox.data

import androidx.room.Room
import dev.zakyymuh.timebox.core.plants.PlantColour
import dev.zakyymuh.timebox.core.plants.PlantForm
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
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
class PlantRepositoryTest {

    private val zone = ZoneId.of("Asia/Jakarta")

    // 18:00 in Jakarta on Saturday 2026-11-07.
    private val now = Instant.parse("2026-11-07T11:00:00Z")
    private fun d(day: Int) = LocalDate.of(2026, 11, day)

    private lateinit var db: TimeboxDatabase
    private lateinit var repo: PlantRepository

    private val monstera = PlantForm(
        name = "Monstera", colour = PlantColour.MOSS, repeats = true,
        everyDays = 7, onceDate = d(7), lastWatered = d(1),
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = PlantRepository(db.plantDao(), Clock.fixed(now, zone))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun nextDue() = repo.overviewNow().let { it.due + it.upcoming }.single().nextDue

    @Test
    fun `a new plant is due interval days after the last watered date given`() = runTest {
        repo.save(0, monstera)
        assertEquals(d(8), nextDue())
    }

    @Test
    fun `watering makes it due interval days from today`() = runTest {
        val id = repo.save(0, monstera)
        repo.water(id)
        assertEquals(d(14), nextDue())
    }

    @Test
    fun `watering twice on the same day records one watering`() = runTest {
        val id = repo.save(0, monstera)
        val first = repo.water(id)
        val second = repo.water(id)
        assertEquals(null, second)
        assertEquals(1, db.plantDao().waterings().size)
        // Undo after a double tap removes the one watering: back to unwatered.
        repo.removeWatering(first!!)
        assertEquals(d(8), nextDue())
    }

    @Test
    fun `undoing the only watering restores the previous due date`() = runTest {
        val id = repo.save(0, monstera)
        repo.removeWatering(repo.water(id)!!)
        assertEquals(d(8), nextDue())
    }

    @Test
    fun `editing the interval keeps history and re-plans`() = runTest {
        val id = repo.save(0, monstera)
        repo.water(id)
        assertEquals(id, repo.save(id, monstera.copy(everyDays = 3)))
        assertEquals(d(10), nextDue())
        assertEquals(1, db.plantDao().waterings().size)
        assertEquals(1, db.plantDao().plants().size)
    }

    @Test
    fun `a one-time plant is stored with its date and no interval`() = runTest {
        val id = repo.save(0, monstera.copy(name = " Cactus ", repeats = false, onceDate = d(10)))
        val stored = db.plantDao().plant(id)!!
        assertEquals("Cactus", stored.name)
        assertEquals(d(10), stored.onceDate)
        assertEquals(null, stored.everyDays)
    }

    @Test
    fun `save rejects an invalid form`() = runTest {
        try {
            repo.save(0, monstera.copy(name = "  "))
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
        }
        assertEquals(0, db.plantDao().plants().size)
    }

    @Test
    fun `deleting a plant removes it from the overview`() = runTest {
        val id = repo.save(0, monstera)
        repo.delete(id)
        assertEquals(true, repo.overviewNow().isEmpty)
    }

    @Test
    fun `switching a watered plant to one time keeps it listed`() = runTest {
        val id = repo.save(0, monstera)
        repo.water(id)
        repo.save(id, monstera.copy(repeats = false, onceDate = d(20)))
        assertEquals(d(20), nextDue())
    }

    @Test
    fun `switching an old one-time plant to repeating counts from today`() = runTest {
        // Created a month ago as "once on 10 Oct" and never watered.
        val id = db.plantDao().upsertPlant(
            PlantEntity(name = "Fern", colour = "MOSS", everyDays = null, onceDate = LocalDate.of(2026, 10, 10), anchor = LocalDate.of(2026, 10, 8)),
        )
        repo.save(id, monstera.copy(name = "Fern", repeats = true, everyDays = 7))
        assertEquals(d(14), nextDue())
    }

    private val dueToday = monstera.copy(lastWatered = LocalDate.of(2026, 10, 31))

    @Test
    fun `still moist moves a due plant to tomorrow`() = runTest {
        val id = repo.save(0, dueToday)
        assertEquals(d(7), nextDue())
        assertEquals(null, repo.snooze(id))
        assertEquals(d(8), nextDue())
    }

    @Test
    fun `watering clears the snooze`() = runTest {
        val id = repo.save(0, dueToday)
        repo.snooze(id)
        repo.water(id)
        assertEquals(d(14), nextDue())
        assertEquals(null, db.plantDao().plant(id)!!.snoozedUntil)
    }

    @Test
    fun `undoing a snooze restores the previous date`() = runTest {
        val id = repo.save(0, dueToday)
        val previous = repo.snooze(id)
        repo.restoreSnooze(id, previous)
        assertEquals(d(7), nextDue())
    }

    @Test
    fun `use suggested interval re-plans`() = runTest {
        val id = repo.save(0, dueToday)
        repo.setInterval(id, 9)
        assertEquals(d(9), nextDue())
    }
}
