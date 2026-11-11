package dev.zakyymuh.timebox.data

import androidx.room.Room
import dev.zakyymuh.timebox.core.collection.DexForm
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
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
    fun `a plant added with a photo starts its diary with it, dated today`() = runTest {
        val id = repo.add(DexForm("Greenovia"), owned = true, photo = "g.jpg")
        val shot = repo.photosFlow(id).first().single()
        assertEquals("g.jpg", shot.file)
        assertEquals("", shot.note)
        assertEquals(today, shot.takenOn)
    }

    @Test
    fun `a plant added without a photo has an empty diary`() = runTest {
        val id = repo.add(DexForm("Greenovia"), owned = true, photo = null)
        assertEquals(emptyList<Any>(), repo.photosFlow(id).first())
    }

    @Test
    fun `a new photo keeps the old one and becomes the cover`() = runTest {
        val id = repo.add(DexForm("Greenovia"), owned = true, photo = "old.jpg")
        repo.addPhoto(id, "new.jpg", "first new leaf!")
        assertEquals("new.jpg", repo.entry(id)?.photo)
        assertEquals(emptyList<String>(), deleted)
        val shots = repo.photosFlow(id).first()
        assertEquals(listOf("new.jpg", "old.jpg"), shots.map { it.file }) // newest first
        assertEquals("first new leaf!", shots.first().note)
    }

    @Test
    fun `a photo note is tidied before it is kept`() = runTest {
        val id = repo.add(DexForm("Greenovia"), owned = true, photo = null)
        repo.addPhoto(id, "a.jpg", "  so   tall  ")
        assertEquals("so tall", repo.photosFlow(id).first().single().note)
    }

    @Test
    fun `a photo's note can be changed later, tidied, and only that photo changes`() = runTest {
        val id = repo.add(DexForm("Greenovia"), owned = true, photo = "a.jpg")
        repo.addPhoto(id, "b.jpg", "tall")
        val (b, a) = repo.photosFlow(id).first()
        repo.setPhotoNote(a.id, "  first   pot ")
        val shots = repo.photosFlow(id).first()
        assertEquals(listOf("tall", "first pot"), shots.map { it.note })
        assertEquals(listOf(b.id, a.id), shots.map { it.id }) // order and files unchanged
        assertEquals(listOf("b.jpg", "a.jpg"), shots.map { it.file })
    }

    @Test
    fun `several photos at once all join the diary, the last picked becomes the cover`() = runTest {
        val id = repo.add(DexForm("Greenovia"), owned = true, photo = "old.jpg")
        repo.addPhotos(id, listOf("a.jpg", "b.jpg", "c.jpg"), " repotted ")
        assertEquals("c.jpg", repo.entry(id)?.photo)
        val shots = repo.photosFlow(id).first()
        assertEquals(listOf("c.jpg", "b.jpg", "a.jpg", "old.jpg"), shots.map { it.file })
        assertEquals(listOf("repotted", "repotted", "repotted", ""), shots.map { it.note })
    }

    @Test
    fun `deleting the cover photo removes its file, and the newest one left becomes the cover`() = runTest {
        val id = repo.add(DexForm("Greenovia"), owned = true, photo = "old.jpg")
        repo.addPhotos(id, listOf("a.jpg", "b.jpg"), "")
        val cover = repo.photosFlow(id).first().first()
        repo.deletePhoto(cover.id)
        assertEquals(listOf("a.jpg", "old.jpg"), repo.photosFlow(id).first().map { it.file })
        assertEquals("a.jpg", repo.entry(id)?.photo)
        assertEquals(listOf("b.jpg"), deleted)
    }

    @Test
    fun `deleting an older photo keeps the cover`() = runTest {
        val id = repo.add(DexForm("Greenovia"), owned = true, photo = "old.jpg")
        repo.addPhoto(id, "new.jpg", "")
        val old = repo.photosFlow(id).first().last()
        repo.deletePhoto(old.id)
        assertEquals("new.jpg", repo.entry(id)?.photo)
        assertEquals(listOf("old.jpg"), deleted)
    }

    @Test
    fun `deleting the last photo leaves the plant without one`() = runTest {
        val id = repo.add(DexForm("Greenovia"), owned = true, photo = "only.jpg")
        repo.deletePhoto(repo.photosFlow(id).first().single().id)
        assertEquals(null, repo.entry(id)?.photo)
        assertTrue(repo.photosFlow(id).first().isEmpty())
        assertEquals(listOf("only.jpg"), deleted)
    }

    @Test
    fun `several photos for a plant that is gone are all thrown away`() = runTest {
        repo.addPhotos(99, listOf("x.jpg", "y.jpg"), "")
        assertEquals(listOf("x.jpg", "y.jpg"), deleted)
    }

    @Test
    fun `a photo for a plant that is gone is thrown away`() = runTest {
        repo.addPhoto(99, "stray.jpg", "")
        assertEquals(listOf("stray.jpg"), deleted)
    }

    @Test
    fun `each plant keeps only its own photos`() = runTest {
        val a = repo.add(DexForm("Greenovia"), owned = true, photo = "a.jpg")
        val b = repo.add(DexForm("Kikko"), owned = true, photo = "b.jpg")
        assertEquals(listOf("a.jpg"), repo.photosFlow(a).first().map { it.file })
        assertEquals(listOf("b.jpg"), repo.photosFlow(b).first().map { it.file })
    }

    @Test
    fun `deleting an entry deletes every one of its photos`() = runTest {
        val id = repo.add(DexForm("Greenovia"), owned = true, photo = "g.jpg")
        repo.addPhoto(id, "g2.jpg", "")
        repo.delete(id)
        assertEquals(null, repo.entry(id))
        assertEquals(setOf("g.jpg", "g2.jpg"), deleted.toSet())
        assertEquals(emptyList<Any>(), repo.photosFlow(id).first())
    }

    @Test
    fun `a wish keeps the price range it was given, and an edit can change it`() = runTest {
        val id = repo.add(DexForm("Kikko", priceLow = 150_000, priceHigh = 300_000), owned = false, photo = null)
        assertEquals(150_000L, repo.entry(id)?.priceLow)
        assertEquals(300_000L, repo.entry(id)?.priceHigh)
        repo.update(id, DexForm("Kikko", priceLow = 120_000))
        assertEquals(120_000L, repo.entry(id)?.priceLow)
        assertEquals(null, repo.entry(id)?.priceHigh)
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
