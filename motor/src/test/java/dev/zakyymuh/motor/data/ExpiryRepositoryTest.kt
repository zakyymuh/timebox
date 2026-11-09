package dev.zakyymuh.motor.data

import androidx.room.Room
import dev.zakyymuh.timebox.core.motor.ExpiryForm
import dev.zakyymuh.timebox.core.motor.VehicleForm
import dev.zakyymuh.timebox.core.motor.VehicleType
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
class ExpiryRepositoryTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-09T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private lateinit var db: MotorDatabase
    private lateinit var repo: ExpiryRepository
    private lateinit var vehicles: VehicleRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), MotorDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = ExpiryRepository(db.expiryDao(), clock)
        vehicles = VehicleRepository(db.vehicleDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `yours and a vehicle's sit in one list, soonest first`() = runTest {
        val avanza = vehicles.save(0, VehicleForm(VehicleType.CAR, "Avanza"))
        repo.save(0, ExpiryForm(null, "SIM C", LocalDate.of(2029, 5, 1), 60))
        repo.save(0, ExpiryForm(avanza, " STNK tax ", LocalDate.of(2027, 3, 12), 12))

        val all = repo.expiriesFlow().first()
        assertEquals(listOf("STNK tax", "SIM C"), all.map { it.name })
        assertEquals(avanza, all.first().vehicleId)
        assertEquals(null, all.last().vehicleId)
    }

    @Test
    fun `renewing moves the date on one period, and can be taken back`() = runTest {
        val id = repo.save(0, ExpiryForm(null, "SIM C", LocalDate.of(2026, 11, 20), 60))
        assertEquals(LocalDate.of(2031, 11, 20), repo.renew(id))
        assertEquals(LocalDate.of(2031, 11, 20), repo.expiry(id)?.date)
        repo.setDate(id, LocalDate.of(2026, 11, 20))
        assertEquals(LocalDate.of(2026, 11, 20), repo.expiry(id)?.date)
    }

    @Test
    fun `a reminder is due once, and renewing starts the next pair`() = runTest {
        val avanza = vehicles.save(0, VehicleForm(VehicleType.CAR, "Avanza"))
        // 9 Nov 2026 in Jakarta; 16 Nov is a week out.
        val id = repo.save(0, ExpiryForm(avanza, "STNK tax", LocalDate.of(2026, 11, 16), 12))
        repo.save(0, ExpiryForm(null, "SIM C", LocalDate.of(2027, 6, 1), 60))
        val reminders = ReminderRepository(db.reminderDao(), repo, vehicles, clock)

        val due = reminders.dueToday().single()
        assertEquals("STNK tax Avanza expires in 1 week, 16 Nov", due.title)
        reminders.markSent(due)
        reminders.markSent(due)
        assertEquals(emptyList<Any>(), reminders.dueToday())

        repo.setDate(id, LocalDate.of(2026, 12, 1))
        assertEquals("STNK tax Avanza expires in 22 days, 1 Dec", reminders.dueToday().single().title)
    }

    @Test
    fun `deleting a vehicle takes its dates but leaves yours`() = runTest {
        val vario = vehicles.save(0, VehicleForm(VehicleType.MOTORCYCLE, "Vario"))
        repo.save(0, ExpiryForm(vario, "STNK tax", LocalDate.of(2027, 1, 1), 12))
        repo.save(0, ExpiryForm(null, "SIM C", LocalDate.of(2029, 1, 1), 60))
        vehicles.delete(vario)
        assertEquals(listOf("SIM C"), repo.expiries().map { it.name })
    }
}
