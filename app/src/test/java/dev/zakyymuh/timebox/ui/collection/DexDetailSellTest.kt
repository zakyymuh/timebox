package dev.zakyymuh.timebox.ui.collection

import androidx.room.Room
import dev.zakyymuh.timebox.core.collection.DexForm
import dev.zakyymuh.timebox.core.collection.SaleForm
import dev.zakyymuh.timebox.core.collection.SaleKind
import dev.zakyymuh.timebox.data.DexRepository
import dev.zakyymuh.timebox.data.PhotoStore
import dev.zakyymuh.timebox.data.SaleRepository
import dev.zakyymuh.timebox.data.TimeboxDatabase
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DexDetailSellTest {

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

    private fun vm(id: Long) = DexDetailViewModel(dex, PhotoStore(RuntimeEnvironment.getApplication()), id, sales)

    @Test
    fun `marking for sale shows on the page`() = runBlocking {
        val id = dex.add(DexForm("Astro"), owned = true, photo = null)
        val vm = vm(id)
        vm.setForSale(true, 150_000)
        val e = withTimeout(5_000) { vm.state.first { it.entry?.forSale == true } }.entry!!
        assertEquals(150_000L, e.askingPrice)
    }

    @Test
    fun `recording a whole sale from the page marks the plant sold`() = runBlocking {
        val id = dex.add(DexForm("Astro"), owned = true, photo = null)
        val vm = vm(id)
        var ok = false
        vm.recordSale(SaleForm(SaleKind.WHOLE, price = 150_000, soldOn = LocalDate.of(2026, 11, 15))) { ok = it }
        val e = withTimeout(5_000) { vm.state.first { it.entry?.soldOn != null } }.entry!!
        assertEquals(LocalDate.of(2026, 11, 15), e.soldOn)
        assertTrue(ok)
    }
}
