package dev.zakyymuh.timebox.core.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DexPriceTest {
    @Test fun `no price shows nothing`() = assertNull(Dex.price(null, null))
    @Test fun `one price is shown in rupiah with dots`() = assertEquals("Rp 150.000", Dex.price(150_000, null))
    @Test fun `only the upper field filled still shows one price`() = assertEquals("Rp 85.000", Dex.price(null, 85_000))
    @Test fun `a range shows both ends`() = assertEquals("Rp 150.000 – 1.250.000", Dex.price(150_000, 1_250_000))
    @Test fun `a range typed backwards is shown low to high`() = assertEquals("Rp 50.000 – 90.000", Dex.price(90_000, 50_000))
    @Test fun `the same price twice is one price`() = assertEquals("Rp 75.000", Dex.price(75_000, 75_000))

    @Test fun `a price can be left out`() = assertTrue(DexForm(name = "Kikko").isValid)
    @Test fun `a negative price is invalid`() = assertFalse(DexForm(name = "Kikko", priceLow = -1).isValid)
    @Test fun `an absurd price is invalid`() = assertFalse(DexForm(name = "Kikko", priceHigh = DexForm.MAX_PRICE + 1).isValid)
}
