package dev.zakyymuh.timebox.data

import androidx.room.Room
import dev.zakyymuh.timebox.TestClock
import dev.zakyymuh.timebox.core.plants.PlantColour
import dev.zakyymuh.timebox.core.plants.PlantForm
import dev.zakyymuh.timebox.core.soil.AirTestForm
import dev.zakyymuh.timebox.core.soil.SampleStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SoilRepositoryTest {
    private val clock = TestClock(Instant.parse("2026-11-01T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private lateinit var db: TimeboxDatabase
    private lateinit var soil: SoilRepository
    private lateinit var plants: PlantRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        soil = SoilRepository(db.airTestDao(), db.plantDao(), clock)
        plants = PlantRepository(db.plantDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    private fun form(name: String) = PlantForm(
        name = name, colour = PlantColour.MOSS, repeats = true, everyDays = 3,
        onceDate = LocalDate.of(2026, 11, 1), lastWatered = LocalDate.of(2026, 10, 25),
    )

    private fun nextDay() {
        clock.now = clock.now.plus(Duration.ofDays(1))
    }

    @Test
    fun `before any test the reminder has not started, even with waterings`() = runTest {
        plants.water(plants.save(0, form("Basil")))
        assertEquals(SampleStatus.NotStarted, soil.statusNow())
        assertEquals(SampleStatus.NotStarted, soil.statusFlow().first())
    }

    @Test
    fun `two waterings of any plants after a test make the sample due`() = runTest {
        val basil = plants.save(0, form("Basil"))
        val cactus = plants.save(0, form("Cactus"))
        plants.water(basil)                     // before the test: doesn't count
        nextDay()
        soil.save(AirTestForm(mix = "Mix A", potMl = 500, drainedMl = 110))
        assertEquals(SampleStatus.Waiting(2), soil.statusNow())

        nextDay()
        plants.water(basil)
        assertEquals(SampleStatus.Waiting(1), soil.statusNow())
        plants.water(cactus)
        assertEquals(SampleStatus.Due(2), soil.statusNow())
        assertEquals(SampleStatus.Due(2), soil.statusFlow().first())
    }

    @Test
    fun `saving a new test resets the count`() = runTest {
        val basil = plants.save(0, form("Basil"))
        soil.save(AirTestForm(potMl = 500, drainedMl = 110))
        nextDay(); plants.water(basil)
        nextDay(); plants.water(basil)
        assertEquals(SampleStatus.Due(2), soil.statusNow())

        nextDay()
        soil.save(AirTestForm(potMl = 500, drainedMl = 100))
        assertEquals(SampleStatus.Waiting(2), soil.statusNow())
        assertEquals(listOf(20, 22), soil.testsFlow().first().map { it.result.airPct })
    }
}
