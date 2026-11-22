package dev.zakyymuh.timebox.data

import androidx.room.Room
import dev.zakyymuh.timebox.TestClock
import dev.zakyymuh.timebox.core.light.LightBand
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LightRepositoryTest {
    private val clock = TestClock(Instant.parse("2026-11-22T05:41:00Z"), ZoneId.of("Asia/Jakarta"))
    private lateinit var db: TimeboxDatabase
    private lateinit var light: LightRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        light = LightRepository(db.lightDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `saving keeps the lux, its band and the clock's time`() = runTest {
        light.save(12_300f)
        val saved = db.lightDao().all().single()
        assertEquals(12_300f, saved.lux)
        assertEquals("Bright", saved.band)
        assertEquals(Instant.parse("2026-11-22T05:41:00Z"), saved.takenAt)
    }

    @Test
    fun `each save is its own reading, newest first`() = runTest {
        light.save(3_100f)
        clock.now = clock.now.plus(Duration.ofHours(1))
        light.save(25_000f)
        assertEquals(listOf("Sun", "Medium"), db.lightDao().all().map { it.band })
    }

    @Test
    fun `a negative or NaN reading is saved as 0, too dark`() = runTest {
        light.save(-5f)
        light.save(Float.NaN)
        val saved = db.lightDao().all()
        assertEquals(listOf(0f, 0f), saved.map { it.lux })
        assertEquals(listOf("Dark", "Dark"), saved.map { it.band })
    }

    @Test
    fun `leaving the screen mid-save still keeps the reading`() = runTest {
        val job = launch(start = CoroutineStart.UNDISPATCHED) { light.save(4_000f) }
        job.cancel()
        job.join()
        assertEquals(listOf(4_000f), db.lightDao().all().map { it.lux })
    }

    @Test
    fun `the saved list shows each reading's lux and band, newest first, and grows with each save`() = runTest {
        assertEquals(emptyList<SavedLight>(), light.readingsFlow().first())
        light.save(420f)
        clock.now = clock.now.plus(Duration.ofMinutes(5))
        light.save(12_300f)
        assertEquals(
            listOf(SavedLight(2, 12_300f, LightBand.Bright), SavedLight(1, 420f, LightBand.Dark)),
            light.readingsFlow().first(),
        )
    }

    @Test
    fun `a band name the app no longer knows reads as too dark`() = runTest {
        db.lightDao().insert(LightReadingEntity(lux = 5f, band = "Gloomy", takenAt = clock.instant()))
        assertEquals(LightBand.Dark, light.readingsFlow().first().single().band)
    }

    @Test
    fun `clearing empties the saved list, and saving starts it again`() = runTest {
        light.save(420f)
        light.save(3_100f)
        light.clear()
        assertEquals(emptyList<SavedLight>(), light.readingsFlow().first())
        light.save(12_300f)
        assertEquals(listOf(12_300f), light.readingsFlow().first().map { it.lux })
    }
}
