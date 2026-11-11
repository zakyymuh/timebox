package dev.zakyymuh.timebox.ui.breeding

import androidx.room.Room
import dev.zakyymuh.timebox.TestClock
import dev.zakyymuh.timebox.core.collection.DexEntry
import dev.zakyymuh.timebox.core.collection.DexForm
import dev.zakyymuh.timebox.data.BreedingRepository
import dev.zakyymuh.timebox.data.DexRepository
import dev.zakyymuh.timebox.data.TimeboxDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BreedingNewViewModelTest {

    private val clock = TestClock(Instant.parse("2026-11-11T03:00:00Z"), ZoneId.of("Asia/Jakarta"))
    private lateinit var db: TimeboxDatabase
    private lateinit var dex: DexRepository
    private lateinit var repo: BreedingRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TimeboxDatabase::class.java)
            .allowMainThreadQueries().build()
        dex = DexRepository(db.dexDao(), clock) {}
        repo = BreedingRepository(db.breedingDao(), db.dexDao(), clock)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private suspend fun parents(vm: BreedingNewViewModel): List<DexEntry> =
        withTimeout(5_000) { vm.parents.filterNotNull().first { it.isNotEmpty() } }

    private suspend fun model(): BreedingNewViewModel {
        dex.add(DexForm("Kikko"), owned = true, photo = null)
        dex.add(DexForm("Aloe"), owned = true, photo = null)
        dex.add(DexForm("Dream"), owned = false, photo = null)
        return BreedingNewViewModel(repo, dex)
    }

    @Test
    fun `only plants in the collection can be parents`() = runBlocking {
        assertEquals(listOf("Kikko", "Aloe"), parents(model()).map { it.name })
    }

    @Test
    fun `picking both parents fills in the name`() = runBlocking {
        val vm = model()
        val (kikko, aloe) = parents(vm)
        vm.pickMother(kikko)
        vm.pickFather(aloe)
        assertEquals("Kikko × Aloe", vm.form.value.name)
        assertTrue(vm.form.value.isValid)
    }

    @Test
    fun `a typed name stays when a parent changes`() = runBlocking {
        val vm = model()
        val (kikko, aloe) = parents(vm)
        vm.pickMother(kikko)
        vm.pickFather(aloe)
        vm.rename("Pink star")
        vm.pickMother(aloe)
        assertEquals("Pink star", vm.form.value.name)
    }

    @Test
    fun `the same plant twice is not a cross`() = runBlocking {
        val vm = model()
        val kikko = parents(vm).first()
        vm.pickMother(kikko)
        vm.pickFather(kikko)
        assertFalse(vm.form.value.isValid)
    }

    @Test
    fun `saving keeps the picked date, today by default`() = runBlocking {
        val vm = model()
        val (kikko, aloe) = parents(vm)
        assertEquals(LocalDate.of(2026, 11, 11), vm.date.value)
        vm.pickMother(kikko)
        vm.pickFather(aloe)
        vm.setDate(LocalDate.of(2026, 10, 30))
        var saved = 0L
        vm.save { saved = it }
        withTimeout(5_000) { while (saved == 0L) kotlinx.coroutines.delay(10) }
        val cross = repo.cross(saved)!!
        assertEquals("1026001", cross.code)
        assertEquals(LocalDate.of(2026, 10, 30), cross.date)
    }
}
