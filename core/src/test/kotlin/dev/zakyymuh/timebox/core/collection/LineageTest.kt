package dev.zakyymuh.timebox.core.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LineageTest {
    @Test fun `children are numbered from one after their parent`() =
        assertEquals(listOf("Haworthia #1", "Haworthia #2", "Haworthia #3"), Lineage.names("Haworthia", 3, emptyList()))

    @Test fun `a second repot counts on after the first`() =
        assertEquals(
            listOf("Haworthia #3", "Haworthia #4"),
            Lineage.names("Haworthia", 2, listOf("Haworthia", "Haworthia #1", "Haworthia #2", "Kikko #9")),
        )

    @Test fun `a long name is cut so every child fits the name limit`() {
        val names = Lineage.names("A".repeat(60), 12, emptyList())
        assertTrue(names.all { it.length <= DexForm.MAX_NAME })
        assertEquals("#12", names.last().takeLast(3))
    }

    @Test fun `the count is kept between one and the most`() {
        assertEquals(1, Lineage.count(0))
        assertEquals(7, Lineage.count(7))
        assertEquals(Lineage.MAX_COUNT, Lineage.count(500))
    }

    @Test fun `a repotted plant reads from its one parent`() =
        assertEquals("Kikko", DexOrigin(1, null, "Kikko", null, null).label)

    @Test fun `a seedling reads from both parents`() =
        assertEquals("Kikko × Hakuun", DexOrigin(1, 2, "Kikko", "Hakuun", 5).label)
}
