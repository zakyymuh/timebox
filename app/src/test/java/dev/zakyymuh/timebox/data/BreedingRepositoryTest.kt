package dev.zakyymuh.timebox.data

import androidx.room.Room
import dev.zakyymuh.timebox.core.collection.BreedingForm
import dev.zakyymuh.timebox.core.collection.DexForm
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
class BreedingRepositoryTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-11T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private val nov = LocalDate.of(2026, 11, 11)
    private lateinit var db: TimeboxDatabase
    private lateinit var dex: DexRepository
    private lateinit var repo: BreedingRepository
    private var kikko = 0L
    private var aloe = 0L

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        dex = DexRepository(db.dexDao(), clock) { }
        repo = BreedingRepository(db.breedingDao(), db.dexDao(), clock)
        kikko = dex.add(DexForm("Kikko"), owned = true, photo = "k.jpg")
        aloe = dex.add(DexForm("Aloe"), owned = true, photo = null)
    }

    @After
    fun tearDown() = db.close()

    private fun form(name: String = "Kikko × Aloe") = BreedingForm(motherId = kikko, fatherId = aloe, name = name)

    @Test
    fun `a cross gets a code from its month, year and count`() = runTest {
        val a = repo.add(form(), nov)!!
        val b = repo.add(form(), nov.minusDays(3))!!
        val c = repo.add(form(), LocalDate.of(2026, 12, 1))!!
        assertEquals("1126001", repo.cross(a)?.code)
        assertEquals("1126002", repo.cross(b)?.code)
        assertEquals("1226001", repo.cross(c)?.code)
    }

    @Test
    fun `a cross keeps its parents and a tidy name`() = runTest {
        val id = repo.add(form("  Kikko × Aloe  "), nov)!!
        val cross = repo.cross(id)!!
        assertEquals("Kikko × Aloe", cross.name)
        assertEquals(kikko, cross.motherId)
        assertEquals(aloe, cross.fatherId)
        assertEquals("Kikko", cross.motherName)
        assertEquals("Aloe", cross.fatherName)
        assertEquals(nov, cross.date)
    }

    @Test
    fun `an invalid cross is not saved`() = runTest {
        assertNull(repo.add(BreedingForm(motherId = kikko, fatherId = kikko, name = "Self"), nov))
        assertNull(repo.add(BreedingForm(motherId = kikko, fatherId = 999, name = "Ghost"), nov))
        assertEquals(emptyList<Any>(), repo.crossesFlow().first())
    }

    @Test
    fun `the list is newest first by date`() = runTest {
        repo.add(form("Old"), nov.minusDays(9))
        repo.add(form("New"), nov)
        repo.add(form("Middle"), nov.minusDays(2))
        assertEquals(listOf("New", "Middle", "Old"), repo.crossesFlow().first().map { it.name })
    }

    @Test
    fun `a parent leaving the collection keeps the cross and its name`() = runTest {
        val id = repo.add(form(), nov)!!
        dex.delete(kikko)
        val cross = repo.cross(id)!!
        assertNull(cross.motherId)
        assertEquals("Kikko", cross.motherName)
        assertEquals(aloe, cross.fatherId)
    }
}
