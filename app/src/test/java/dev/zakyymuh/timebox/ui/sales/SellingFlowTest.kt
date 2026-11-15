package dev.zakyymuh.timebox.ui.sales

import androidx.room.Room
import dev.zakyymuh.timebox.core.collection.DexForm
import dev.zakyymuh.timebox.core.collection.SaleKind
import dev.zakyymuh.timebox.data.DexRepository
import dev.zakyymuh.timebox.data.PhotoStore
import dev.zakyymuh.timebox.data.SaleRepository
import dev.zakyymuh.timebox.data.TimeboxDatabase
import dev.zakyymuh.timebox.ui.collection.CollectionViewModel
import dev.zakyymuh.timebox.ui.collection.DexDetailViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
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

/** Collection → $ → Sales › For sale → Sold (or Cancel sale), through the view models. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SellingFlowTest {

    private val today = LocalDate.of(2026, 11, 15)
    private val clock = Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private lateinit var db: TimeboxDatabase
    private lateinit var dex: DexRepository
    private lateinit var sales: SaleRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        dex = DexRepository(db.dexDao(), clock) {}
        sales = SaleRepository(db, clock)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun detail(id: Long) = DexDetailViewModel(dex, PhotoStore(RuntimeEnvironment.getApplication()), id, sales)

    private suspend fun forSale(id: Long, asking: Long?): DexDetailViewModel {
        val page = detail(id)
        page.setForSale(true, asking)
        withTimeout(5_000) { page.state.first { it.entry?.forSale == true } }
        return page
    }

    @Test
    fun `putting a plant up for sale moves it from the grid to Sales, For sale`() = runBlocking {
        val astro = dex.add(DexForm("Astro"), owned = true, photo = null)
        val kikko = dex.add(DexForm("Kikko"), owned = true, photo = null)
        val grid = CollectionViewModel(dex)
        withTimeout(5_000) { grid.lists.first { it?.grid?.size == 2 } }

        forSale(astro, 150_000)

        val lists = withTimeout(5_000) { grid.lists.first { it?.grid?.size == 1 } }!!
        assertEquals(listOf(kikko), lists.grid.map { it.id })
        val shelf = withTimeout(5_000) { SalesViewModel(sales).state.first { it?.forSale?.isNotEmpty() == true } }!!
        assertEquals(listOf(astro), shelf.forSale.map { it.id })
        assertEquals(today, shelf.forSale.single().forSaleSince)
    }

    @Test
    fun `sold with a negotiated price keeps one whole sale at that price and the plant moves to Sold`() = runBlocking {
        val astro = dex.add(DexForm("Astro"), owned = true, photo = null)
        val page = forSale(astro, 150_000)
        val kept = CompletableDeferred<Boolean>()
        page.sold(negotiated = 120_000) { kept.complete(it) }
        assertTrue(withTimeout(5_000) { kept.await() })

        val state = withTimeout(5_000) { SalesViewModel(sales).state.first { it?.sold?.size == 1 } }!!
        val sale = state.sold.single()
        assertEquals(SaleKind.WHOLE, sale.kind)
        assertEquals(1, sale.quantity)
        assertEquals(120_000L, sale.price)
        assertEquals(today, sale.soldOn)
        assertEquals(astro, sale.dexId)
        assertTrue(state.forSale.isEmpty())
        val e = withTimeout(5_000) { page.state.first { it.entry?.soldOn != null } }.entry!!
        assertTrue(e.readOnly)
    }

    @Test
    fun `sold with an empty price keeps the sale at the asking price`() = runBlocking {
        val astro = dex.add(DexForm("Astro"), owned = true, photo = null)
        forSale(astro, 150_000).sold(negotiated = null) {}

        val state = withTimeout(5_000) { SalesViewModel(sales).state.first { it?.sold?.size == 1 } }!!
        assertEquals(150_000L, state.sold.single().price)
    }

    @Test
    fun `sold with no price at all keeps nothing`() = runBlocking {
        val astro = dex.add(DexForm("Astro"), owned = true, photo = null)
        val kept = CompletableDeferred<Boolean>()
        forSale(astro, null).sold(negotiated = null) { kept.complete(it) }
        assertEquals(false, withTimeout(5_000) { kept.await() })

        val state = withTimeout(5_000) { SalesViewModel(sales).state.first { it != null } }!!
        assertTrue(state.sold.isEmpty())
        assertEquals(listOf(astro), state.forSale.map { it.id })
    }

    @Test
    fun `cancel sale puts the plant back in the grid and off For sale`() = runBlocking {
        val astro = dex.add(DexForm("Astro"), owned = true, photo = null)
        val grid = CollectionViewModel(dex)
        val page = forSale(astro, 150_000)
        withTimeout(5_000) { grid.lists.first { it?.grid?.isEmpty() == true } }

        page.setForSale(false, 150_000)

        val lists = withTimeout(5_000) { grid.lists.first { it?.grid?.size == 1 } }!!
        assertEquals(listOf(astro), lists.grid.map { it.id })
        val shelf = withTimeout(5_000) { SalesViewModel(sales).state.first { it != null } }!!
        assertTrue(shelf.forSale.isEmpty())
        assertTrue(shelf.sold.isEmpty())
    }

    @Test
    fun `a for-sale plant still counts as owned and can still be a parent`() = runBlocking {
        val astro = dex.add(DexForm("Astro"), owned = true, photo = null)
        forSale(astro, 150_000)
        val all = withTimeout(5_000) { dex.entriesFlow().first { list -> list.any { it.forSale } } }
        assertEquals(1, all.count { it.inCollection })
    }
}
