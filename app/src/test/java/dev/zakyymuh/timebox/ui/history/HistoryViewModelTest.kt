package dev.zakyymuh.timebox.ui.history

import androidx.room.Room
import dev.zakyymuh.timebox.TestClock
import dev.zakyymuh.timebox.core.collection.DexForm
import dev.zakyymuh.timebox.core.plants.PlantColour
import dev.zakyymuh.timebox.core.plants.PlantForm
import dev.zakyymuh.timebox.data.DexRepository
import dev.zakyymuh.timebox.data.PlantRepository
import dev.zakyymuh.timebox.data.TimeboxDatabase
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HistoryViewModelTest {

    // 18:00 Saturday 7 Nov in Jakarta.
    private val clock = TestClock(Instant.parse("2026-11-07T11:00:00Z"), ZoneId.of("Asia/Jakarta"))

    private lateinit var db: TimeboxDatabase
    private lateinit var plants: PlantRepository
    private lateinit var dex: DexRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        plants = PlantRepository(db.plantDao(), clock)
        dex = DexRepository(db.dexDao(), clock) {}
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun d(day: Int, month: Int = 11) = LocalDate.of(2026, month, day)

    private suspend fun plant(name: String) = plants.save(0, PlantForm(
        name = name, colour = PlantColour.MOSS, repeats = true, everyDays = 3, onceDate = d(7), lastWatered = d(4),
    ))

    private suspend fun HistoryViewModel.await(check: (HistoryState) -> Boolean) =
        withTimeout(5_000) { state.first(check) }

    @Test
    fun `the history groups waterings and arrivals by date`() = runBlocking {
        plants.water(plant("Basil"))
        dex.add(DexForm("Monstera"), owned = true, photo = null, acquired = d(20, 10))
        val state = HistoryViewModel(plants, dex, clock).await { s -> s.groups.sumOf { it.events.size } == 2 }
        assertEquals(listOf("Today", "October 2026"), state.groups.map { it.title })
    }

    @Test
    fun `search narrows the list as you type`() = runBlocking {
        plants.water(plant("Basil"))
        plants.water(plant("Aloe"))
        val model = HistoryViewModel(plants, dex, clock)
        model.await { s -> s.groups.sumOf { it.events.size } == 2 }
        model.search("bas")
        val state = model.await { it.query == "bas" && it.groups.sumOf { g -> g.events.size } == 1 }
        assertEquals("Watered Basil", state.groups.single().events.single().text)
        model.search("cactus")
        assertEquals("Nothing matches ‘cactus’", model.await { it.query == "cactus" && it.empty != null }.empty)
    }
}
