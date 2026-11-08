package dev.zakyymuh.timebox.core.soil

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AirSpaceTest {
    @Test fun `air space is drained water over pot volume`() {
        assertEquals(22, AirSpace.result(potMl = 500, drainedMl = 110, pouredMl = null).airPct)
    }

    @Test fun `percentages round to the nearest whole number`() {
        assertEquals(33, AirSpace.result(300, 100, null).airPct)   // 33.3
        assertEquals(67, AirSpace.result(300, 200, null).airPct)   // 66.7
    }

    @Test fun `poured water adds total porosity and water holding`() {
        val r = AirSpace.result(potMl = 500, drainedMl = 110, pouredMl = 250)
        assertEquals(50, r.totalPct)
        assertEquals(28, r.waterPct)
    }

    @Test fun `without poured water there is no total or water holding`() {
        val r = AirSpace.result(500, 110, null)
        assertNull(r.totalPct)
        assertNull(r.waterPct)
    }

    @Test fun `verdicts follow the air space`() {
        assertTrue(AirSpace.verdict(5).startsWith("Dense"))
        assertTrue(AirSpace.verdict(15).startsWith("Moderate"))
        assertTrue(AirSpace.verdict(25).startsWith("Airy"))
        assertTrue(AirSpace.verdict(40).startsWith("Very airy"))
    }

    @Test fun `a form needs a pot volume and drained water no more than the pot`() {
        assertTrue(AirTestForm(potMl = 500, drainedMl = 110).isValid)
        assertFalse(AirTestForm(potMl = null, drainedMl = 110).isValid)
        assertFalse(AirTestForm(potMl = 500, drainedMl = null).isValid)
        assertFalse(AirTestForm(potMl = 0, drainedMl = 0).isValid)
        assertFalse(AirTestForm(potMl = 500, drainedMl = 600).isValid)
    }

    @Test fun `poured water must be at least what drained and at most the pot`() {
        assertTrue(AirTestForm(potMl = 500, drainedMl = 110, pouredMl = 250).isValid)
        assertFalse(AirTestForm(potMl = 500, drainedMl = 110, pouredMl = 100).isValid)
        assertFalse(AirTestForm(potMl = 500, drainedMl = 110, pouredMl = 501).isValid)
    }

    @Test fun `a valid form has a result, an invalid one does not`() {
        assertEquals(22, AirTestForm(potMl = 500, drainedMl = 110).result?.airPct)
        assertNull(AirTestForm(potMl = 500, drainedMl = 600).result)
    }
}
