package dev.zakyymuh.timebox.core.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DexTest {
    private val today = LocalDate.of(2026, 11, 8)
    private fun entry(id: Long, number: Int, owned: Boolean) =
        DexEntry(id = id, number = number, name = "P$number", species = "", owned = owned, photo = null, acquired = null, note = "")

    @Test fun `numbers look like a dex`() {
        assertEquals("#001", Dex.label(1))
        assertEquals("#042", Dex.label(42))
        assertEquals("#1234", Dex.label(1234))
    }

    @Test fun `the first entry is number one`() = assertEquals(1, Dex.nextNumber(emptyList()))

    @Test fun `numbers are never reused, even after a delete`() {
        // #2 was deleted; the next one is still after the highest.
        assertEquals(4, Dex.nextNumber(listOf(1, 3)))
    }

    @Test fun `getting a wished plant keeps its number and dates it today`() {
        val got = Dex.gotIt(entry(5, 7, owned = false), today)
        assertTrue(got.owned)
        assertEquals(7, got.number)
        assertEquals(today, got.acquired)
    }

    @Test fun `summary counts both lists`() {
        val all = listOf(entry(1, 1, true), entry(2, 2, true), entry(3, 3, true), entry(4, 4, false), entry(5, 5, false))
        assertEquals("3 plants · 2 on wishlist", Dex.summary(all))
    }

    @Test fun `summary is singular for one`() {
        assertEquals("1 plant · 1 on wishlist", Dex.summary(listOf(entry(1, 1, true), entry(2, 2, false))))
    }

    @Test fun `summary for an empty dex invites you in`() {
        assertEquals("Start your collection", Dex.summary(emptyList()))
    }

    @Test fun `summary without a wishlist leaves it out`() {
        assertEquals("2 plants", Dex.summary(listOf(entry(1, 1, true), entry(2, 2, true))))
    }

    @Test fun `a form needs a name`() {
        assertFalse(DexForm(name = "  ").isValid)
        assertTrue(DexForm(name = "Greenovia").isValid)
    }

    @Test fun `a name over the limit is invalid`() {
        assertFalse(DexForm(name = "x".repeat(DexForm.MAX_NAME + 1)).isValid)
    }
}
