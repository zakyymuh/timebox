package dev.zakyymuh.timebox.core.collection

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DexSortTest {
    private fun e(number: Int, name: String, acquired: LocalDate? = null, low: Long? = null, high: Long? = null) =
        DexEntry(number.toLong(), number, name, "", acquired != null, null, acquired, "", low, high)

    private fun d(day: Int) = LocalDate.of(2026, 11, day)

    @Test fun `by name goes A to Z, ignoring case`() {
        val list = listOf(e(1, "kikko"), e(2, "Astrophytum"), e(3, "Greenovia"))
        assertEquals(listOf("Astrophytum", "Greenovia", "kikko"), DexSort.Name.apply(list).map { it.name })
    }

    @Test fun `by date puts the plant got most recently first`() {
        val list = listOf(e(1, "A", d(3)), e(2, "B", d(9)), e(3, "C", d(5)))
        assertEquals(listOf("B", "C", "A"), DexSort.Date.apply(list).map { it.name })
    }

    @Test fun `by date, a same-day tie or an undated wish goes newest added first`() {
        val list = listOf(e(1, "A"), e(2, "B"), e(3, "C", d(5)), e(4, "D", d(5)))
        assertEquals(listOf("D", "C", "B", "A"), DexSort.Date.apply(list).map { it.name })
    }

    @Test fun `by price goes cheapest first, and plants with no price go last`() {
        val list = listOf(e(1, "A", low = 300_000), e(2, "B"), e(3, "C", low = 50_000, high = 900_000), e(4, "D", high = 120_000))
        assertEquals(listOf("C", "D", "A", "B"), DexSort.Price.apply(list).map { it.name })
    }

    @Test fun `each sort has a short label`() =
        assertEquals(listOf("Newest", "A–Z", "Cheapest"), DexSort.entries.map { it.label })
}
