package dev.zakyymuh.timebox.data

import androidx.room.Room
import dev.zakyymuh.timebox.core.collection.BreedingForm
import dev.zakyymuh.timebox.core.collection.DexForm
import dev.zakyymuh.timebox.core.collection.DexOrigin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LineageRepositoryTest {

    private val today = LocalDate.of(2026, 11, 13)
    private val clock = Clock.fixed(Instant.parse("2026-11-13T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private lateinit var db: TimeboxDatabase
    private lateinit var dex: DexRepository
    private lateinit var breeding: BreedingRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        dex = DexRepository(db.dexDao(), clock) {}
        breeding = BreedingRepository(db.breedingDao(), db.dexDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `a repot adds numbered children of the parent's kind, and the parent stays`() = runTest {
        val bulk = dex.add(DexForm("Haworthia", species = "H. cooperi"), owned = true, photo = null)
        val ids = dex.repot(bulk, 3)

        val kids = ids.map { dex.entry(it)!! }
        assertEquals(listOf("Haworthia #1", "Haworthia #2", "Haworthia #3"), kids.map { it.name })
        assertEquals(listOf(2, 3, 4), kids.map { it.number })
        assertTrue(kids.all { it.owned && it.species == "H. cooperi" && it.acquired == today })
        assertEquals(DexOrigin(bulk, null, "Haworthia", null, null), dex.originFlow(ids[0]).first())
        assertEquals(ids, dex.childrenFlow(bulk).first().map { it.id })
        assertTrue(dex.entry(bulk) != null)
    }

    @Test
    fun `a second repot counts on`() = runTest {
        val mother = dex.add(DexForm("Kikko"), owned = true, photo = null)
        dex.repot(mother, 2)
        val more = dex.repot(mother, 2)
        assertEquals(listOf("Kikko #3", "Kikko #4"), more.map { dex.entry(it)!!.name })
        assertEquals(4, dex.childrenFlow(mother).first().size)
    }

    @Test
    fun `an emptied bulk pot can leave, and its children keep its name`() = runTest {
        val bulk = dex.add(DexForm("Bulk"), owned = true, photo = null)
        val ids = dex.repot(bulk, 2, removeParent = true)
        assertNull(dex.entry(bulk))
        assertEquals(DexOrigin(null, null, "Bulk", null, null), dex.originFlow(ids[0]).first())
    }

    @Test
    fun `a wish cannot be repotted`() = runTest {
        val wish = dex.add(DexForm("Wish"), owned = false, photo = null)
        assertEquals(emptyList<Long>(), dex.repot(wish, 3))
    }

    @Test
    fun `seedlings of a cross remember both parents and the cross`() = runTest {
        val m = dex.add(DexForm("Kikko"), owned = true, photo = null)
        val f = dex.add(DexForm("Hakuun"), owned = true, photo = null)
        val crossId = breeding.add(BreedingForm(m, f, "Kikko × Hakuun"), today)!!
        val ids = dex.sow(breeding.cross(crossId)!!, 2)

        assertEquals(listOf("Kikko × Hakuun #1", "Kikko × Hakuun #2"), ids.map { dex.entry(it)!!.name })
        assertEquals(DexOrigin(m, f, "Kikko", "Hakuun", crossId), dex.originFlow(ids[0]).first())
        assertEquals(ids, dex.childrenFlow(m).first().map { it.id })
        assertEquals(ids, dex.childrenFlow(f).first().map { it.id })
        assertEquals(ids, dex.seedlingsFlow(crossId).first().map { it.id })
    }

    @Test
    fun `deleting a child takes its origin with it`() = runTest {
        val mother = dex.add(DexForm("Kikko"), owned = true, photo = null)
        val kid = dex.repot(mother, 1).single()
        dex.delete(kid)
        assertEquals(emptyList<Any>(), dex.childrenFlow(mother).first())
    }
}
