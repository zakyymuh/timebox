package dev.zakyymuh.timebox.data

import androidx.room.Room
import dev.zakyymuh.timebox.core.motor.ServiceForm
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ServiceRepositoryTest {
    private val today = LocalDate.of(2026, 11, 8)
    private val clock = Clock.fixed(Instant.parse("2026-11-08T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private lateinit var db: TimeboxDatabase
    private lateinit var repo: ServiceRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = ServiceRepository(db.serviceDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `a service is saved with when, where and price, newest first`() = runTest {
        repo.save(0, ServiceForm(date = LocalDate.of(2026, 6, 1), place = "Bengkel Jaya", price = 90_000))
        repo.save(0, ServiceForm(date = today, place = " AHASS ", price = 150_000, work = "oil + brake pads", odometerKm = 12_000))
        val records = repo.recordsFlow().first()
        assertEquals(listOf("AHASS", "Bengkel Jaya"), records.map { it.place })
        assertEquals(150_000L, records.first().price)
        assertEquals(12_000, records.first().odometerKm)
    }

    @Test
    fun `editing changes the record in place, deleting removes it`() = runTest {
        val id = repo.save(0, ServiceForm(date = today, place = "AHASS", price = 150_000))
        repo.save(id, ServiceForm(date = today, place = "AHASS", price = 175_000))
        assertEquals(175_000L, repo.record(id)?.price)
        assertEquals(1, repo.recordsFlow().first().size)
        repo.delete(id)
        assertEquals(0, repo.recordsFlow().first().size)
    }
}
