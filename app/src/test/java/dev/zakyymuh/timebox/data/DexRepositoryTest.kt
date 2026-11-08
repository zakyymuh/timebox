package dev.zakyymuh.timebox.data

import androidx.room.Room
import dev.zakyymuh.timebox.core.collection.DexForm
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
class DexRepositoryTest {

    private val today = LocalDate.of(2026, 11, 8)
    private val clock = Clock.fixed(Instant.parse("2026-11-08T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private val deleted = mutableListOf<String>()
    private lateinit var db: TimeboxDatabase
    private lateinit var repo: DexRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = DexRepository(db.dexDao(), clock) { deleted += it }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `entries are numbered in the order they are added, across both lists`() = runTest {
        repo.add(DexForm("Greenovia"), owned = true, photo = null)
        repo.add(DexForm("Astrophytum asterias"), owned = false, photo = null)
        repo.add(DexForm("Myriostigma"), owned = true, photo = null)
        assertEquals(listOf(1, 2, 3), db.dexDao().entries().map { it.number })
    }

    @Test
    fun `two adds at once never share a number`() = runTest {
        (1..5).map { async { repo.add(DexForm("P$it"), owned = true, photo = null) } }.awaitAll()
        assertEquals(listOf(1, 2, 3, 4, 5), db.dexDao().entries().map { it.number })
    }

    @Test
    fun `a collected plant is dated today, a wished one is not dated`() = runTest {
        val got = repo.add(DexForm("Greenovia"), owned = true, photo = null)
        val wish = repo.add(DexForm("Variegata"), owned = false, photo = null)
        assertEquals(today, repo.entry(got)?.acquired)
        assertEquals(null, repo.entry(wish)?.acquired)
    }

    @Test
    fun `got it moves a wish into the collection with the same number`() = runTest {
        repo.add(DexForm("Greenovia"), owned = true, photo = null)
        val wish = repo.add(DexForm("Variegata"), owned = false, photo = null)
        repo.gotIt(wish)
        val entry = repo.entry(wish)!!
        assertTrue(entry.owned)
        assertEquals(2, entry.number)
        assertEquals(today, entry.acquired)
    }

    @Test
    fun `a new photo replaces the old file`() = runTest {
        val id = repo.add(DexForm("Greenovia"), owned = true, photo = "old.jpg")
        repo.setPhoto(id, "new.jpg")
        assertEquals("new.jpg", repo.entry(id)?.photo)
        assertEquals(listOf("old.jpg"), deleted)
    }

    @Test
    fun `deleting an entry deletes its photo`() = runTest {
        val id = repo.add(DexForm("Greenovia"), owned = true, photo = "g.jpg")
        repo.delete(id)
        assertEquals(null, repo.entry(id))
        assertEquals(listOf("g.jpg"), deleted)
    }

    @Test
    fun `editing keeps the number and photo`() = runTest {
        val id = repo.add(DexForm("Greenovia"), owned = true, photo = "g.jpg")
        repo.update(id, DexForm("Greenovia dodrentalis", species = "Crassulaceae", note = "from the market"))
        val entry = repo.entry(id)!!
        assertEquals("Greenovia dodrentalis", entry.name)
        assertEquals(1, entry.number)
        assertEquals("g.jpg", entry.photo)
        assertEquals("from the market", entry.note)
    }
}
