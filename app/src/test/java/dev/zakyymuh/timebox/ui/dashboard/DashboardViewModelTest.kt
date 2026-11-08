package dev.zakyymuh.timebox.ui.dashboard

import androidx.room.Room
import dev.zakyymuh.timebox.TestClock
import dev.zakyymuh.timebox.core.DaySummary
import dev.zakyymuh.timebox.core.collection.DexForm
import dev.zakyymuh.timebox.core.plants.FunFacts
import dev.zakyymuh.timebox.data.DexRepository
import dev.zakyymuh.timebox.core.motor.ServiceForm
import dev.zakyymuh.timebox.data.PlantRepository
import dev.zakyymuh.timebox.data.ServiceRepository
import dev.zakyymuh.timebox.data.TimeboxDatabase
import dev.zakyymuh.timebox.data.TimeboxRepository
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
    private lateinit var timebox: TimeboxRepository
    private lateinit var plants: PlantRepository
    private lateinit var dex: DexRepository
    private lateinit var service: ServiceRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        timebox = TimeboxRepository(db.dao(), clock)
        plants = PlantRepository(db.plantDao(), clock)
        dex = DexRepository(db.dexDao(), clock) {}
        service = ServiceRepository(db.serviceDao(), clock)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun vm(random: Random = Random(1)) = DashboardViewModel(timebox, plants, dex, service, random)

    private suspend fun DashboardViewModel.await(check: (DashboardState) -> Boolean) =
        withTimeout(5_000) { state.first(check) }

    @Test
    fun `the habit card counts what is done out of the total, with a cheer`() = runBlocking {
        timebox.addItem("cuci piring")
        timebox.addItem("nyuci baju")
        timebox.setDone(timebox.todayNow().items.first().id, true)
        val state = vm().await { it.habit == DaySummary(1, 2) }
        assertEquals("Over halfway!", state.cheer)
    }

    @Test
    fun `the collection card sums up both lists`() = runBlocking {
        dex.add(DexForm("Greenovia"), owned = true, photo = "g.jpg")
        dex.add(DexForm("Variegata"), owned = false, photo = null)
        val state = vm().await { it.collection == "1 plant · 1 on wishlist" }
        assertEquals(listOf("g.jpg"), state.thumbnails)
    }

    @Test
    fun `the motorcycle card shows the last service`() = runBlocking {
        vm().await { it.motor == "No service logged yet" }
        service.save(0, ServiceForm(date = java.time.LocalDate.of(2026, 10, 31), place = "AHASS", price = 150_000))
        vm().await { it.motor == "Last: 31 Oct at AHASS · 7 days ago" }
        Unit
    }

    @Test
    fun `the fun fact is drawn at random when the app opens`() = runBlocking {
        val state = vm(Random(3)).await { it.date != null }
        assertEquals(FunFacts.random(Random(3)), state.fact)
    }

    @Test
    fun `closing the fun fact hides it`() = runBlocking {
        val model = vm()
        model.await { it.fact != null }
        model.closeFact()
        model.await { it.date != null && it.fact == null }
        Unit
    }

    @Test
    fun `after midnight refresh shows the new day`() = runBlocking {
        timebox.addItem("cuci piring") // written on the 7th
        val model = vm()
        model.await { it.habit == DaySummary(0, 1) }

        clock.now = afterMidnight
        model.refresh()

        model.await { it.habit == DaySummary(0, 0) && it.cheer == "Plan your day" }
        Unit
    }
}
