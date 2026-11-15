package dev.zakyymuh.timebox.core.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Round 2: whole plants only, a for-sale plant off the grid, Sold with a negotiated price. */
class SalesV2Test {
    private val nov15 = LocalDate.of(2026, 11, 15)

    private fun entry(owned: Boolean = true, forSale: Boolean = false, soldOn: LocalDate? = null) =
        DexEntry(1, 1, "Astro", "", owned, null, null, "", forSale = forSale, askingPrice = 150_000, soldOn = soldOn)

    @Test fun `a for-sale plant leaves the grid but is still owned`() {
        val e = entry(forSale = true)
        assertFalse(e.inGrid)
        assertTrue(e.inCollection)
    }

    @Test fun `a plant not for sale is in the grid`() = assertTrue(entry().inGrid)
    @Test fun `a sold plant is not in the grid`() = assertFalse(entry(soldOn = nov15).inGrid)
    @Test fun `a wish is not in the grid`() = assertFalse(entry(owned = false).inGrid)

    @Test fun `the grid keeps plants in the collection that are not for sale`() {
        val kept = entry().copy(id = 2)
        val listed = entry(forSale = true).copy(id = 3)
        val sold = entry(soldOn = nov15).copy(id = 4)
        val wish = entry(owned = false).copy(id = 5)
        assertEquals(listOf(kept), Dex.grid(listOf(kept, listed, sold, wish)))
    }

    @Test fun `the negotiated price wins`() = assertEquals(120_000L, Sales.soldPrice(negotiated = 120_000, asking = 150_000))
    @Test fun `no negotiated price falls back to the asking price`() = assertEquals(150_000L, Sales.soldPrice(negotiated = null, asking = 150_000))
    @Test fun `a negotiated price works without an asking price`() = assertEquals(90_000L, Sales.soldPrice(negotiated = 90_000, asking = null))
    @Test fun `no price at all cannot be sold`() = assertNull(Sales.soldPrice(negotiated = null, asking = null))
    @Test fun `a negotiated price above the max cannot be sold`() = assertNull(Sales.soldPrice(negotiated = DexForm.MAX_PRICE + 1, asking = 1))

    @Test fun `a sold-today form is one whole plant at the settled price with no note`() = assertEquals(
        SaleForm(kind = SaleKind.WHOLE, quantity = 1, price = 150_000, soldOn = nov15, note = ""),
        Sales.soldToday(negotiated = null, asking = 150_000, today = nov15),
    )

    @Test fun `no sold-today form without a price`() = assertNull(Sales.soldToday(negotiated = null, asking = null, today = nov15))

    @Test fun `a share is an instagram story`() {
        assertEquals(1080, Story.WIDTH)
        assertEquals(1920, Story.HEIGHT)
        assertEquals(9 * Story.HEIGHT, 16 * Story.WIDTH)
    }

    @Test fun `the story photo ends about 330px above the bottom`() = assertEquals(1590f, Story.PHOTO_BOTTOM)
}
