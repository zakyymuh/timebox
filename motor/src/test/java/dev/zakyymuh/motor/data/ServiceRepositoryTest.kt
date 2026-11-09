package dev.zakyymuh.motor.data

import androidx.room.Room
import dev.zakyymuh.timebox.core.motor.ServiceForm
import dev.zakyymuh.timebox.core.motor.VehicleForm
import dev.zakyymuh.timebox.core.motor.VehicleType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    private lateinit var db: MotorDatabase
    private lateinit var repo: ServiceRepository
    private lateinit var vehicles: VehicleRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), MotorDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = ServiceRepository(db.serviceDao(), clock)
        vehicles = VehicleRepository(db.vehicleDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun vario() = vehicles.save(0, VehicleForm(VehicleType.MOTORCYCLE, "Vario", "b 1234 xyz"))

    @Test
    fun `a service is saved with when, where and price, newest first`() = runTest {
        val vario = vario()
        repo.save(0, vario, ServiceForm(date = LocalDate.of(2026, 6, 1), place = "Bengkel Jaya", price = 90_000))
        repo.save(0, vario, ServiceForm(date = today, place = " AHASS ", price = 150_000, work = "oil + brake pads", odometerKm = 12_000))
        val records = repo.recordsFlow(vario).first()
        assertEquals(listOf("AHASS", "Bengkel Jaya"), records.map { it.place })
        assertEquals(150_000L, records.first().price)
        assertEquals(12_000, records.first().odometerKm)
    }

    @Test
    fun `editing changes the record in place, deleting removes it`() = runTest {
        val vario = vario()
        val id = repo.save(0, vario, ServiceForm(date = today, place = "AHASS", price = 150_000))
        repo.save(id, vario, ServiceForm(date = today, place = "AHASS", price = 175_000))
        assertEquals(175_000L, repo.record(id)?.price)
        assertEquals(1, repo.recordsFlow(vario).first().size)
        repo.delete(id)
        assertEquals(0, repo.recordsFlow(vario).first().size)
    }

    @Test
    fun `each vehicle keeps its own log`() = runTest {
        val vario = vario()
        val avanza = vehicles.save(0, VehicleForm(VehicleType.CAR, "Avanza"))
        repo.save(0, vario, ServiceForm(date = today, place = "AHASS"))
        repo.save(0, avanza, ServiceForm(date = today, place = "Toyota"))
        repo.save(0, avanza, ServiceForm(date = today, place = "Shop & Drive"))

        assertEquals(listOf("AHASS"), repo.recordsFlow(vario).first().map { it.place })
        val byVehicle = repo.byVehicleFlow().first()
        assertEquals(1, byVehicle[vario]?.size)
        assertEquals(2, byVehicle[avanza]?.size)
    }

    @Test
    fun `a vehicle is saved with a tidy plate, and deleting it takes its log`() = runTest {
        val vario = vario()
        assertEquals("B 1234 XYZ", vehicles.vehicle(vario)?.plate)
        vehicles.save(vario, VehicleForm(VehicleType.MOTORCYCLE, " Vario 125 ", "B 1234 XYZ", photo = "v.jpg"))
        assertEquals("Vario 125", vehicles.vehicle(vario)?.name)
        assertEquals("v.jpg", vehicles.vehicle(vario)?.photo)

        repo.save(0, vario, ServiceForm(date = today, place = "AHASS"))
        vehicles.delete(vario)
        assertNull(vehicles.vehicle(vario))
        assertEquals(0, repo.recordsFlow(vario).first().size)
        assertEquals(emptyList<Any>(), vehicles.vehiclesFlow().first())
    }
}
