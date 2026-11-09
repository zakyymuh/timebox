package dev.zakyymuh.timebox.core.motor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleTest {

    @Test
    fun `plates are capitalised with single spaces`() {
        assertEquals("B 1234 XYZ", Plate.normalise("  b  1234\txyz "))
        assertEquals("", Plate.normalise("   "))
    }

    @Test
    fun `a vehicle needs a name, and the plate is optional`() {
        assertFalse(VehicleForm(name = "  ").isValid())
        assertTrue(VehicleForm(name = "Vario").isValid())
        assertTrue(VehicleForm(name = "Vario", plate = "b 1234 xyz").isValid())
        assertFalse(VehicleForm(name = "x".repeat(VehicleForm.MAX_NAME + 1)).isValid())
        assertFalse(VehicleForm(name = "Vario", plate = "B 1234 XYZ ABCD").isValid())
    }

    @Test
    fun `the title adds the plate when there is one`() {
        assertEquals("Vario · B 1234 XYZ", Vehicle(1, VehicleType.MOTORCYCLE, "Vario", "B 1234 XYZ", null).title)
        assertEquals("Avanza", Vehicle(2, VehicleType.CAR, "Avanza", "", null).title)
    }
}
