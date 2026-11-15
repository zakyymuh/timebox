package dev.zakyymuh.timebox.core.light

import org.junit.Assert.assertEquals
import org.junit.Test

class LightLevelTest {
    @Test fun `bands follow the lux reading`() {
        assertEquals(LightBand.Dark, LightLevel.band(100f))
        assertEquals(LightBand.Low, LightLevel.band(1_000f))
        assertEquals(LightBand.Medium, LightLevel.band(5_000f))
        assertEquals(LightBand.Bright, LightLevel.band(15_000f))
        assertEquals(LightBand.Sun, LightLevel.band(40_000f))
    }

    @Test fun `each band starts at its lower edge`() {
        assertEquals(LightBand.Low, LightLevel.band(500f))
        assertEquals(LightBand.Medium, LightLevel.band(2_500f))
        assertEquals(LightBand.Bright, LightLevel.band(10_000f))
        assertEquals(LightBand.Sun, LightLevel.band(20_000f))
    }

    @Test fun `negative or broken readings count as dark`() {
        assertEquals(LightBand.Dark, LightLevel.band(-5f))
        assertEquals(LightBand.Dark, LightLevel.band(Float.NaN))
    }

    @Test fun `lux reads as a short whole number`() {
        assertEquals("0", LightLevel.format(0.4f))
        assertEquals("850", LightLevel.format(850.2f))
        assertEquals("8,400", LightLevel.format(8_437f))
        assertEquals("42,000", LightLevel.format(41_980f))
    }

    @Test fun `smoothing moves part way towards the new reading`() {
        assertEquals(1_000f, LightLevel.smooth(null, 1_000f), 0.01f)
        assertEquals(1_250f, LightLevel.smooth(1_000f, 2_000f), 0.01f)
    }
}
