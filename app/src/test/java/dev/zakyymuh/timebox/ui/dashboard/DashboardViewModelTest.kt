package dev.zakyymuh.timebox.ui.dashboard

import androidx.room.Room
import dev.zakyymuh.timebox.TestClock
import dev.zakyymuh.timebox.core.collection.DexForm
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
    fun `the collection card sums up both lists`() = runBlocking {
        dex.add(DexForm("Greenovia"), owned = true, photo = "g.jpg")
        dex.add(DexForm("Variegata"), owned = false, photo = null)
        val state = vm().await { it.collection == "1 plant · 1 on wishlist" }
        assertEquals(listOf("g.jpg"), state.thumbnails)
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
        val model = vm()
        model.await { it.date == java.time.LocalDate.of(2026, 11, 7) }

        clock.now = afterMidnight
        model.refresh()

        model.await { it.date == java.time.LocalDate.of(2026, 11, 8) }
        Unit
    }
}
