package dev.zakyymuh.timebox.ui.dashboard

import androidx.room.Room
import dev.zakyymuh.timebox.TestClock
import dev.zakyymuh.timebox.core.collection.DexForm
import dev.zakyymuh.timebox.core.plants.DayMark
import dev.zakyymuh.timebox.core.plants.PlantColour
import dev.zakyymuh.timebox.core.plants.PlantForm
import dev.zakyymuh.timebox.core.plants.FunFacts
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DashboardViewModelTest {

    // 18:00 Saturday 7 Nov in Jakarta; +7h is 01:00 Sunday 8 Nov.
    private val clock = TestClock(Instant.parse("2026-11-07T11:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private val afterMidnight = Instant.parse("2026-11-07T18:00:00Z")

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

    private fun vm(random: Random = Random(1)) = DashboardViewModel(plants, dex, clock, random)

    private suspend fun DashboardViewModel.await(check: (DashboardState) -> Boolean) =
        withTimeout(5_000) { state.first(check) }

    @Test
    fun `the collection tile shows the newest owned photo`() = runBlocking {
        dex.add(DexForm("Greenovia"), owned = true, photo = "g.jpg")
        dex.add(DexForm("Variegata"), owned = false, photo = "v.jpg")
        dex.add(DexForm("Aloe"), owned = true, photo = "a.jpg")
        assertEquals("a.jpg", vm().await { it.cover != null }.cover)
    }

    @Test
    fun `the collection tile counts owned plants, not wishes`() = runBlocking {
        dex.add(DexForm("Greenovia"), owned = true, photo = null)
        dex.add(DexForm("Variegata"), owned = false, photo = null)
        dex.add(DexForm("Aloe"), owned = true, photo = null)
        assertEquals("2 plants", vm().await { it.owned == "2 plants" }.owned)
    }

    @Test
    fun `the fun fact is drawn at random when the app opens`() = runBlocking {
        val state = vm(Random(3)).await { it.date != null }
        assertEquals(FunFacts.random(Random(3)), state.fact)
    }

    @Test
    fun `tapping the fun fact shows another one`() = runBlocking {
        val model = vm()
        val first = model.await { it.fact != null }.fact
        model.shuffleFact()
        model.await { it.fact != null && it.fact != first }
        Unit
    }

    @Test
    fun `no plant event shows until something has happened`() = runBlocking {
        assertNull(vm().await { it.date != null }.event)
    }

    @Test
    fun `a watering gives a plant event line`() = runBlocking {
        val aloe = plant("Aloe", everyDays = 3, lastWatered = d(4))
        plants.water(aloe)
        val state = vm().await { it.event != null }
        assertTrue(state.event!!.contains("Aloe"))
    }

    @Test
    fun `after midnight refresh shows the new day`() = runBlocking {
        val model = vm()
        model.await { it.date == java.time.LocalDate.of(2026, 11, 7) }

        clock.now = afterMidnight
        model.refresh()

        model.await { it.date == java.time.LocalDate.of(2026, 11, 8) }
        Unit
    }

    private fun d(day: Int) = java.time.LocalDate.of(2026, 11, day)

    private suspend fun plant(name: String, everyDays: Int, lastWatered: java.time.LocalDate) =
        plants.save(0, PlantForm(
            name = name, colour = PlantColour.MOSS, repeats = true,
            everyDays = everyDays, onceDate = d(7), lastWatered = lastWatered,
        ))

    @Test
    fun `the week runs from three days back to three days ahead`() = runBlocking {
        val state = vm().await { it.date != null }
        assertEquals((4..10).map(::d), state.week.map { it.date })
    }

    @Test
    fun `the week shows today's watering and what is coming`() = runBlocking {
        val aloe = plant("Aloe", everyDays = 3, lastWatered = d(4)) // due today, the 7th
        plant("Basil", everyDays = 2, lastWatered = d(7)) // due the 9th
        plants.water(aloe)
        val state = vm().await { s -> s.week.any { it.plants.isNotEmpty() } }
        fun on(day: Int) = state.week.single { it.date == d(day) }.plants.map { it.plant.name to it.mark }
        assertEquals(listOf("Aloe" to DayMark.Watered), on(7))
        assertEquals(emptyList<Pair<String, DayMark>>(), on(8))
        assertEquals(listOf("Basil" to DayMark.Coming), on(9))
        assertEquals(listOf("Aloe" to DayMark.Coming), on(10))
    }
}
