package dev.zakyymuh.timebox.ui.dashboard

import androidx.room.Room
import dev.zakyymuh.timebox.TestClock
import dev.zakyymuh.timebox.core.TodayEntry
import dev.zakyymuh.timebox.core.plants.PlantColour
import dev.zakyymuh.timebox.core.plants.PlantForm
import dev.zakyymuh.timebox.data.PlantRepository
import dev.zakyymuh.timebox.data.TimeboxDatabase
import dev.zakyymuh.timebox.data.TimeboxRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DashboardViewModelTest {

    // 18:00 Saturday 7 Nov in Jakarta; +7h is 01:00 Sunday 8 Nov.
    private val clock = TestClock(Instant.parse("2026-11-07T11:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private val afterMidnight = Instant.parse("2026-11-07T18:00:00Z")
    private fun d(day: Int) = LocalDate.of(2026, 11, day)

    private lateinit var db: TimeboxDatabase
    private lateinit var timebox: TimeboxRepository
    private lateinit var plants: PlantRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        timebox = TimeboxRepository(db.dao(), clock)
        plants = PlantRepository(db.plantDao(), clock)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun form(name: String, every: Int, lastWatered: LocalDate) = PlantForm(
        name = name, colour = PlantColour.MOSS, repeats = true, everyDays = every, onceDate = d(7), lastWatered = lastWatered,
    )

    private fun describe(entries: List<TodayEntry>) = entries.map {
        when (it) {
            is TodayEntry.Water -> "water ${it.row.plant.name}"
            is TodayEntry.Tick -> "tick ${it.item.text}"
        }
    }

    /** Waits until the dashboard shows exactly [expected] on its Today list. */
    private suspend fun DashboardViewModel.awaitToday(vararg expected: String) =
        withTimeout(5_000) { state.first { s -> s.today?.let(::describe) == expected.toList() } }

    @Test
    fun `Today lists due plants then unticked items`() = runBlocking {
        plants.save(0, form("Basil", 3, d(4)))      // due today
        plants.save(0, form("Monstera", 7, d(6)))   // not yet
        timebox.addItem("cuci piring")
        DashboardViewModel(timebox, plants).awaitToday("water Basil", "tick cuci piring")
        Unit
    }

    @Test
    fun `ticking from Today then Undo brings the item back unticked`() = runBlocking {
        timebox.addItem("cuci piring")
        val vm = DashboardViewModel(timebox, plants)
        vm.awaitToday("tick cuci piring")
        val event = async { withTimeout(5_000) { vm.undo.first() } }
        yield()

        vm.tick(timebox.todayNow().items.single().id)
        vm.awaitToday()
        val ticked = event.await() as DashboardUndo.Ticked
        assertEquals("cuci piring", ticked.text)

        vm.undo(ticked)
        vm.awaitToday("tick cuci piring")
        assertEquals(false, timebox.todayNow().items.single().isDone)
    }

    @Test
    fun `watering from Today removes it and Undo restores it`() = runBlocking {
        val id = plants.save(0, form("Basil", 3, d(4)))
        val vm = DashboardViewModel(timebox, plants)
        vm.awaitToday("water Basil")
        val event = async { withTimeout(5_000) { vm.undo.first() } }
        yield()

        vm.water(id)
        vm.awaitToday()
        val watered = event.await() as DashboardUndo.Watered
        assertEquals("Basil", watered.name)

        vm.undo(watered)
        vm.awaitToday("water Basil")
        Unit
    }

    @Test
    fun `after midnight refresh shows the new day`() = runBlocking {
        timebox.addItem("cuci piring")                 // written on the 7th
        plants.save(0, form("Monstera", 7, d(1)))      // due on the 8th
        val vm = DashboardViewModel(timebox, plants)
        vm.awaitToday("tick cuci piring")

        clock.now = afterMidnight
        vm.refresh()

        vm.awaitToday("water Monstera")
        Unit
    }

    @Test
    fun `a double tap on tick leaves the item ticked`() = runBlocking {
        timebox.addItem("cuci piring")
        val vm = DashboardViewModel(timebox, plants)
        vm.awaitToday("tick cuci piring")
        val id = timebox.todayNow().items.single().id

        vm.tick(id)
        vm.tick(id)
        vm.awaitToday()
        // Give any second coroutine time to land before checking.
        kotlinx.coroutines.delay(300)

        assertEquals(true, timebox.todayNow().items.single().isDone)
    }

    @Test
    fun `a double tap on water records one watering`() = runBlocking {
        val id = plants.save(0, form("Basil", 3, d(4)))
        val vm = DashboardViewModel(timebox, plants)
        vm.awaitToday("water Basil")

        vm.water(id)
        vm.water(id)
        vm.awaitToday()
        kotlinx.coroutines.delay(300)

        assertEquals(1, db.plantDao().waterings().size)
    }

    @Test
    fun `the fun fact is about a plant you have`() = runBlocking {
        plants.save(0, form("Greenovia", 4, d(7)))
        val vm = DashboardViewModel(timebox, plants)
        val state = withTimeout(5_000) { vm.state.first { it.fact?.entry?.id == "greenovia" } }
        assertEquals("greenovia", state.fact?.entry?.id)
    }
}
