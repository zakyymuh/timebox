package dev.zakyymuh.timebox.core.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SalesTest {
    private val nov15 = LocalDate.of(2026, 11, 15)

    private fun sale(kind: SaleKind, quantity: Int, price: Long, on: LocalDate = nov15, name: String = "Astro") =
        Sale(id = 0, dexId = 1, plantName = name, kind = kind, quantity = quantity, price = price, soldOn = on, note = "")

    private fun entry(owned: Boolean, soldOn: LocalDate? = null) =
        DexEntry(1, 1, "Astro", "", owned, null, null, "", soldOn = soldOn)

    @Test fun `totals add quantities and prices`() = assertEquals(
        SalesTotals(items = 4, earned = 210_000, monthItems = 3, monthEarned = 60_000),
        Sales.totals(
            listOf(
                sale(SaleKind.PUP, 3, 60_000, LocalDate.of(2026, 11, 2)),
                sale(SaleKind.WHOLE, 1, 150_000, LocalDate.of(2026, 10, 30)),
            ),
            nov15,
        ),
    )

    @Test fun `a sale on the last day of last month is not this month`() =
        assertEquals(0, Sales.totals(listOf(sale(SaleKind.PUP, 1, 10_000, LocalDate.of(2026, 10, 31))), LocalDate.of(2026, 11, 1)).monthItems)

    @Test fun `no sales is all zero`() = assertEquals(SalesTotals(0, 0, 0, 0), Sales.totals(emptyList(), nov15))

    @Test fun `line for pups`() = assertEquals("2 pups from Astro · Rp 50.000", Sales.line(sale(SaleKind.PUP, 2, 50_000)))

    @Test fun `line for one cutting`() = assertEquals("1 cutting from Astro · Rp 25.000", Sales.line(sale(SaleKind.CUTTING, 1, 25_000)))

    @Test fun `line for seedlings`() = assertEquals("3 seedlings from Astro · Rp 90.000", Sales.line(sale(SaleKind.SEEDLING, 3, 90_000)))

    @Test fun `line for whole plant`() = assertEquals("Astro · Rp 150.000", Sales.line(sale(SaleKind.WHOLE, 1, 150_000)))

    @Test fun `suggested price multiplies asking by quantity`() {
        assertEquals(120_000L, Sales.suggestedPrice(40_000, 3))
        assertNull(Sales.suggestedPrice(null, 3))
    }

    @Test fun `normalise forces whole plant to quantity 1`() =
        assertEquals(1, Sales.normalise(SaleForm(SaleKind.WHOLE, quantity = 4, price = 1, soldOn = nov15)).quantity)

    @Test fun `normalise keeps a pup quantity`() =
        assertEquals(4, Sales.normalise(SaleForm(SaleKind.PUP, quantity = 4, price = 1, soldOn = nov15)).quantity)

    @Test fun `form with a price is valid`() = assertTrue(SaleForm(price = 0, soldOn = nov15).isValid)
    @Test fun `form needs a price`() = assertFalse(SaleForm(price = null, soldOn = nov15).isValid)
    @Test fun `form rejects quantity 0`() = assertFalse(SaleForm(SaleKind.PUP, quantity = 0, price = 1, soldOn = nov15).isValid)
    @Test fun `form rejects quantity 100`() = assertFalse(SaleForm(SaleKind.PUP, quantity = 100, price = 1, soldOn = nov15).isValid)
    @Test fun `form rejects price above max`() = assertFalse(SaleForm(price = DexForm.MAX_PRICE + 1, soldOn = nov15).isValid)
    @Test fun `form rejects a long note`() = assertFalse(SaleForm(price = 1, soldOn = nov15, note = "x".repeat(301)).isValid)

    @Test fun `a sold plant is not in the collection`() = assertFalse(entry(owned = true, soldOn = nov15).inCollection)
    @Test fun `an owned plant is in the collection`() = assertTrue(entry(owned = true).inCollection)
    @Test fun `a wish is not in the collection`() = assertFalse(entry(owned = false).inCollection)

    @Test fun `dashboard line with no sales at all`() = assertEquals("Nothing sold yet", Sales.dashboardLine(emptyList(), nov15))

    @Test fun `dashboard line shows this month's total`() = assertEquals(
        "Rp 420.000 this month",
        Sales.dashboardLine(listOf(sale(SaleKind.PUP, 2, 420_000), sale(SaleKind.WHOLE, 1, 99_000, LocalDate.of(2026, 10, 1))), nov15),
    )

    @Test fun `dashboard line when nothing sold this month`() =
        assertEquals("Rp 0 this month", Sales.dashboardLine(listOf(sale(SaleKind.PUP, 1, 10_000, LocalDate.of(2026, 10, 1))), nov15))
}
