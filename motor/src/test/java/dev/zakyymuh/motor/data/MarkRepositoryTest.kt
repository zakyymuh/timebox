package dev.zakyymuh.motor.data

import androidx.room.Room
import dev.zakyymuh.timebox.core.lastdone.LastDone
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
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MarkRepositoryTest {
    private val jakarta = ZoneId.of("Asia/Jakarta")
    private var now = Instant.parse("2026-11-09T03:00:00Z")
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = jakarta
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now
    }
    private lateinit var db: MotorDatabase
    private lateinit var repo: MarkRepository
    private lateinit var vehicles: VehicleRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), MotorDatabase::class.java)
            .allowMainThreadQueries().build()
        vehicles = VehicleRepository(db.vehicleDao(), clock)
        repo = MarkRepository(db.markDao(), vehicles, clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `a vehicle's chore is named after it, and two vehicles count apart`() = runTest {
        val vario = vehicles.save(0, VehicleForm(VehicleType.MOTORCYCLE, "Vario"))
        val avanza = vehicles.save(0, VehicleForm(VehicleType.CAR, "Avanza"))
        repo.markDone("Cuci motor", vario)
        repo.markDone("Cuci motor", avanza)
        repo.markDone(" SIM for car ", null)

        val log = repo.logFlow().first()
        assertEquals(setOf("Cuci motor (Vario)", "Cuci motor (Avanza)", "SIM for car"), log.events.map { it.name }.toSet())
        assertEquals(MarkTarget("Cuci motor", avanza), log.targets[LastDone.key("Cuci motor (Avanza)")])
        assertEquals(MarkTarget("SIM for car", null), log.targets["sim for car"])
    }

    @Test
    fun `undo takes back only the latest of that one event`() = runTest {
        val vario = vehicles.save(0, VehicleForm(VehicleType.MOTORCYCLE, "Vario"))
        repo.markDone("Tambah angin", vario)
        now = now.plusSeconds(86_400 * 12)
        repo.markDone("Tambah angin", vario)
        repo.markDone("Tambah angin", null)

        repo.undoLastMark(LastDone.key("Tambah angin (Vario)"))
        val events = repo.logFlow().first().events.associateBy { it.name }
        assertEquals(1, events.getValue("Tambah angin (Vario)").count)
        assertEquals(1, events.getValue("Tambah angin").count)
        // The undone mark was today's, so the Vario's last one is back to 12 days ago.
        assertEquals("12 days ago", LastDone.ago(events.getValue("Tambah angin (Vario)").lastDay, repo.todayDate()))
    }

    @Test
    fun `deleting a vehicle takes its chores`() = runTest {
        val vario = vehicles.save(0, VehicleForm(VehicleType.MOTORCYCLE, "Vario"))
        repo.markDone("Cuci motor", vario)
        repo.markDone("SIM for car", null)
        vehicles.delete(vario)
        assertEquals(listOf("SIM for car"), repo.logFlow().first().events.map { it.name })
    }
}
