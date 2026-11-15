package dev.zakyymuh.timebox.ui.sales

import androidx.room.Room
import dev.zakyymuh.timebox.core.collection.SaleForm
import dev.zakyymuh.timebox.core.collection.SaleKind
import dev.zakyymuh.timebox.core.collection.SalesTotals
import dev.zakyymuh.timebox.data.DexEntity
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
class SalesViewModelTest {

    private val today = LocalDate.of(2026, 11, 15)
    private val clock = Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private lateinit var db: TimeboxDatabase
    private lateinit var repo: SaleRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = SaleRepository(db, clock)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private suspend fun plant(name: String, number: Int): Long = db.dexDao().insert(
        DexEntity(number = number, name = name, species = "", owned = true, photo = null, acquired = today, note = "", createdAt = Instant.EPOCH),
    )

    @Test
    fun `state shows totals, for-sale plants and sales newest first`() = runBlocking {
        val astro = plant("Astro", 1)
        val kikko = plant("Kikko", 2)
        repo.setForSale(kikko, true, 75_000)
        repo.record(astro, SaleForm(SaleKind.PUP, quantity = 2, price = 50_000, soldOn = LocalDate.of(2026, 10, 20)))
        repo.record(astro, SaleForm(SaleKind.WHOLE, price = 150_000, soldOn = LocalDate.of(2026, 11, 3)))

        val state = withTimeout(5_000) { SalesViewModel(repo).state.first { it != null && it.sold.size == 2 } }!!
        assertEquals(SalesTotals(items = 3, earned = 200_000, monthItems = 1, monthEarned = 150_000), state.totals)
        assertEquals(listOf(kikko), state.forSale.map { it.id })
        assertEquals(listOf(SaleKind.WHOLE, SaleKind.PUP), state.sold.map { it.kind })
    }

    @Test
    fun `deleting a sale updates the totals`() = runBlocking {
        val astro = plant("Astro", 1)
        repo.record(astro, SaleForm(SaleKind.PUP, price = 50_000, soldOn = today))
        val vm = SalesViewModel(repo)
        val sale = withTimeout(5_000) { vm.state.first { it?.sold?.size == 1 } }!!.sold.single()
        vm.delete(sale.id)
        val after = withTimeout(5_000) { vm.state.first { it?.sold?.isEmpty() == true } }!!
        assertEquals(SalesTotals(0, 0, 0, 0), after.totals)
    }
}
