package dev.zakyymuh.timebox.core.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BreedingTest {
    private val nov = LocalDate.of(2026, 11, 11)

    @Test fun `the first cross of a month is number one`() =
        assertEquals("1126001", Breeding.code(nov, emptyList()))

    @Test fun `the code counts on within the month`() =
        assertEquals("1126003", Breeding.code(nov, listOf("1126001", "1126002")))

    @Test fun `each month starts its count again`() =
        assertEquals("1226001", Breeding.code(LocalDate.of(2026, 12, 1), listOf("1126001", "1126002")))

    @Test fun `a gap is never filled, so a code is never given twice`() =
        assertEquals("1126004", Breeding.code(nov, listOf("1126001", "1126003")))

    @Test fun `a single-digit month still makes seven characters`() =
        assertEquals("0327001", Breeding.code(LocalDate.of(2027, 3, 2), emptyList()))

    @Test fun `the name comes from both parents`() =
        assertEquals("Kikko × Astrophytum", Breeding.name("Kikko ", " Astrophytum"))

    @Test fun `a cross needs two different parents and a name`() {
        assertTrue(BreedingForm(motherId = 1, fatherId = 2, name = "A × B").isValid)
        assertFalse(BreedingForm(motherId = 1, fatherId = 1, name = "A × A").isValid)
        assertFalse(BreedingForm(motherId = 1, fatherId = null, name = "A").isValid)
        assertFalse(BreedingForm(motherId = 1, fatherId = 2, name = "  ").isValid)
        assertFalse(BreedingForm(motherId = 1, fatherId = 2, name = "x".repeat(BreedingForm.MAX_NAME + 1)).isValid)
    }

    @Test fun `the dashboard card invites a first cross`() =
        assertEquals("Pick two plants from your collection to cross", Breeding.teaser(null))

    @Test fun `the dashboard card names the latest cross`() =
        assertEquals(
            "Latest: Kikko × Aloe · 11 Nov 2026",
            Breeding.teaser(Cross(1, "1126001", "Kikko × Aloe", nov, 1, 2, "Kikko", "Aloe")),
        )
}
