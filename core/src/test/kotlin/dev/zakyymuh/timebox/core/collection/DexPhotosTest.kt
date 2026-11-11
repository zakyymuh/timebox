package dev.zakyymuh.timebox.core.collection

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DexPhotosTest {

    @Test fun `a photo note is trimmed and its spaces squeezed`() =
        assertEquals("first new leaf!", DexPhotos.note("  first   new\nleaf!  "))

    @Test fun `a blank photo note is empty`() = assertEquals("", DexPhotos.note("   "))

    @Test fun `a photo note is cut to fit the bubble`() =
        assertEquals(DexPhotos.MAX_NOTE, DexPhotos.note("a".repeat(200)).length)

    @Test fun `the caption under a photo gives its day and place`() =
        assertEquals("11 Nov 2026 · 2 of 3", DexPhotos.caption(LocalDate.of(2026, 11, 11), index = 1, count = 3))

    @Test fun `a single photo has no count`() =
        assertEquals("11 Nov 2026", DexPhotos.caption(LocalDate.of(2026, 11, 11), index = 0, count = 1))
}
