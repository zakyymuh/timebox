package dev.zakyymuh.timebox.data

import androidx.room.Room
import dev.zakyymuh.timebox.core.collection.SaleForm
import dev.zakyymuh.timebox.core.collection.SaleKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class SaleRepositoryTest {

    private val today = LocalDate.of(2026, 11, 15)
    private val clock = Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private lateinit var db: TimeboxDatabase
    private lateinit var repo: SaleRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = SaleRepository(db, clock)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun plant(owned: Boolean = true, number: Int = 1): Long = db.dexDao().insert(
        DexEntity(number = number, name = "Astro", species = "", owned = owned, photo = null, acquired = today, note = "", createdAt = Instant.EPOCH),
    )

    private fun form(kind: SaleKind, quantity: Int = 1, price: Long? = 50_000, on: LocalDate = today) =
        SaleForm(kind = kind, quantity = quantity, price = price, soldOn = on)

    @Test
    fun `setting for sale stores the asking price`() = runTest {
        val id = plant()
        repo.setForSale(id, forSale = true, askingPrice = 150_000)
        val e = db.dexDao().entry(id)!!
        assertTrue(e.forSale)
        assertEquals(150_000L, e.askingPrice)
        assertEquals(listOf(id), repo.forSaleFlow().first().map { it.id })
    }

    @Test
    fun `a wish cannot be put up for sale`() = runTest {
        val id = plant(owned = false)
        repo.setForSale(id, forSale = true, askingPrice = 150_000)
        assertFalse(db.dexDao().entry(id)!!.forSale)
    }

    @Test
    fun `selling a pup keeps the plant in the collection and for sale`() = runTest {
        val id = plant()
        repo.setForSale(id, true, 25_000)
        assertNotNull(repo.record(id, form(SaleKind.PUP, quantity = 2)))
        val e = db.dexDao().entry(id)!!
        assertNull(e.soldOn)
        assertTrue(e.forSale)
        assertEquals(2, repo.salesFlow().first().single().quantity)
    }

    @Test
    fun `selling the whole plant marks it sold on the sale date and takes it off for sale`() = runTest {
        val id = plant()
        repo.setForSale(id, true, 150_000)
        val on = LocalDate.of(2026, 11, 10)
        assertNotNull(repo.record(id, form(SaleKind.WHOLE, quantity = 3, on = on)))
        val e = db.dexDao().entry(id)!!
        assertEquals(on, e.soldOn)
        assertFalse(e.forSale)
        assertTrue(repo.forSaleFlow().first().isEmpty())
        assertEquals(1, repo.salesFlow().first().single().quantity)
    }

    @Test
    fun `a sold plant cannot be sold whole again`() = runTest {
        val id = plant()
        repo.record(id, form(SaleKind.WHOLE))
        assertNull(repo.record(id, form(SaleKind.WHOLE)))
        assertEquals(1, repo.salesFlow().first().size)
    }

    @Test
    fun `a sold plant cannot be put back up for sale`() = runTest {
        val id = plant()
        repo.record(id, form(SaleKind.WHOLE))
        repo.setForSale(id, true, 1)
        assertFalse(db.dexDao().entry(id)!!.forSale)
    }

    @Test
    fun `deleting the whole sale brings the plant back`() = runTest {
        val id = plant()
        val sale = repo.record(id, form(SaleKind.WHOLE))!!
        repo.delete(sale)
        assertNull(db.dexDao().entry(id)!!.soldOn)
        assertTrue(repo.salesFlow().first().isEmpty())
    }

    @Test
    fun `editing a whole sale into a cutting brings the plant back`() = runTest {
        val id = plant()
        val sale = repo.record(id, form(SaleKind.WHOLE))!!
        repo.edit(sale, form(SaleKind.CUTTING, quantity = 2))
        assertNull(db.dexDao().entry(id)!!.soldOn)
        assertEquals(SaleKind.CUTTING, repo.salesFlow().first().single().kind)
    }

    @Test
    fun `editing a pup into the whole plant marks it sold`() = runTest {
        val id = plant()
        val sale = repo.record(id, form(SaleKind.PUP))!!
        repo.edit(sale, form(SaleKind.WHOLE))
        assertEquals(today, db.dexDao().entry(id)!!.soldOn)
    }

    @Test
    fun `deleting the plant keeps its sales by name`() = runTest {
        val id = plant()
        repo.record(id, form(SaleKind.PUP))
        db.dexDao().delete(id)
        val sale = repo.salesFlow().first().single()
        assertNull(sale.dexId)
        assertEquals("Astro", sale.plantName)
    }

    @Test
    fun `record refuses an invalid form`() = runTest {
        val id = plant()
        assertNull(repo.record(id, form(SaleKind.PUP, quantity = 0)))
        assertNull(repo.record(id, form(SaleKind.PUP, price = null)))
        assertTrue(repo.salesFlow().first().isEmpty())
    }

    @Test
    fun `a wish cannot be sold`() = runTest {
        val id = plant(owned = false)
        assertNull(repo.record(id, form(SaleKind.PUP)))
    }
}
