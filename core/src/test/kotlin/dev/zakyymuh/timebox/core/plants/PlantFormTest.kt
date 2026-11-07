package dev.zakyymuh.timebox.core.plants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlantFormTest {

    private val today = LocalDate.of(2026, 11, 7)
    private val ok = PlantForm(
        name = "Basil", colour = PlantColour.DUSK, repeats = true,
        everyDays = 3, onceDate = today, lastWatered = today,
    )

    @Test
    fun `a sensible form is valid`() = assertTrue(ok.isValid(today))

    @Test
    fun `blank or whitespace name is rejected`() {
        assertEquals(setOf(FormError.NAME_BLANK), ok.copy(name = "").errors(today))
        assertEquals(setOf(FormError.NAME_BLANK), ok.copy(name = "   ").errors(today))
    }

    @Test
    fun `name over 40 characters is rejected, 40 is fine, measured after trimming`() {
        assertEquals(setOf(FormError.NAME_TOO_LONG), ok.copy(name = "x".repeat(41)).errors(today))
        assertTrue(ok.copy(name = "  " + "x".repeat(40) + "  ").isValid(today))
    }

    @Test
    fun `interval 0 and 91 are rejected, 1 and 90 accepted`() {
        assertEquals(setOf(FormError.INTERVAL_OUT_OF_RANGE), ok.copy(everyDays = 0).errors(today))
        assertEquals(setOf(FormError.INTERVAL_OUT_OF_RANGE), ok.copy(everyDays = 91).errors(today))
        assertTrue(ok.copy(everyDays = 1).isValid(today))
        assertTrue(ok.copy(everyDays = 90).isValid(today))
    }

    @Test
    fun `last watered in the future is rejected`() {
        assertEquals(setOf(FormError.LAST_WATERED_IN_FUTURE), ok.copy(lastWatered = today.plusDays(1)).errors(today))
    }

    @Test
    fun `interval and last watered are not checked for one-time plants`() {
        assertTrue(ok.copy(repeats = false, everyDays = 0, lastWatered = today.plusDays(5)).isValid(today))
    }
}
