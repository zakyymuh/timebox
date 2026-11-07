package dev.zakyymuh.timebox.nudge

import androidx.room.Room
import dev.zakyymuh.timebox.TestClock
import dev.zakyymuh.timebox.core.plants.PlantColour
import dev.zakyymuh.timebox.core.plants.PlantForm
import dev.zakyymuh.timebox.data.PlantRepository
import dev.zakyymuh.timebox.data.TimeboxDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PlantActionsTest {

    // 18:00 Saturday 7 Nov in Jakarta — when the reminder fires.
    private val clock = TestClock(Instant.parse("2026-11-07T11:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private fun d(day: Int) = LocalDate.of(2026, 11, day)

    private lateinit var db: TimeboxDatabase
    private lateinit var repo: PlantRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = PlantRepository(db.plantDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun basil() = repo.save(0, PlantForm(
        name = "Basil", colour = PlantColour.DUSK, repeats = true,
        everyDays = 3, onceDate = d(7), lastWatered = d(4),
    ))

    @Test
    fun `water action records a watering`() = runBlocking {
        val id = basil()
        assertEquals(true, PlantActions.handle(repo, PlantActions.ACTION_WATER, id))
        assertEquals(1, db.plantDao().waterings().size)
    }

    @Test
    fun `check action snoozes to tomorrow`() = runBlocking {
        val id = basil()
        assertEquals(true, PlantActions.handle(repo, PlantActions.ACTION_CHECK, id))
        assertEquals(d(8), db.plantDao().plant(id)!!.snoozedUntil)
    }

    @Test
    fun `unknown plant does nothing and returns false`() = runBlocking {
        assertEquals(false, PlantActions.handle(repo, PlantActions.ACTION_WATER, 404))
        assertEquals(0, db.plantDao().waterings().size)
    }

    @Test
    fun `unknown action does nothing`() = runBlocking {
        val id = basil()
        assertEquals(false, PlantActions.handle(repo, "something.else", id))
        assertEquals(0, db.plantDao().waterings().size)
    }
}
