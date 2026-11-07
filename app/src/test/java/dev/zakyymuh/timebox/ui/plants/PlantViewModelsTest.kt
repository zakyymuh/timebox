package dev.zakyymuh.timebox.ui.plants

import androidx.room.Room
import dev.zakyymuh.timebox.data.PlantRepository
import dev.zakyymuh.timebox.data.TimeboxDatabase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
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

/** A clock the test can move past midnight. */
private class TestClock(var now: Instant, private val zone: ZoneId) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = TestClock(now, zone)
    override fun instant(): Instant = now
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PlantViewModelsTest {

    // 18:00 Saturday 7 Nov in Jakarta; +7h is 01:00 Sunday 8 Nov.
    private val clock = TestClock(Instant.parse("2026-11-07T11:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private val afterMidnight = Instant.parse("2026-11-07T18:00:00Z")
    private fun d(day: Int) = LocalDate.of(2026, 11, day)

    private lateinit var db: TimeboxDatabase
    private lateinit var repo: PlantRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = PlantRepository(db.plantDao(), clock)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `saving twice quickly creates one plant and finishes once`() = runBlocking {
        val vm = EditPlantViewModel(repo, 0)
        vm.update(vm.form.value.copy(name = "Basil"))
        val done = CompletableDeferred<Unit>()
        var finished = 0

        vm.save { finished++; done.complete(Unit) }
        vm.save { finished++ }
        withTimeout(5_000) { done.await() }

        assertEquals(1, db.plantDao().plants().size)
        assertEquals(1, finished)
    }

    @Test
    fun `the calendar moves to the new day when refreshed after midnight`() = runBlocking {
        val vm = CalendarViewModel(repo)
        assertEquals(d(7), vm.state.value.today)

        clock.now = afterMidnight
        vm.refresh()

        assertEquals(d(8), withTimeout(5_000) { vm.state.first { it.today == d(8) } }.today)
    }

    @Test
    fun `a new plant's untouched dates follow today after midnight`() {
        val vm = EditPlantViewModel(repo, 0)

        clock.now = afterMidnight
        vm.refresh()

        assertEquals(d(8), vm.form.value.lastWatered)
        assertEquals(d(8), vm.form.value.onceDate)
    }
}
