package dev.zakyymuh.timebox.core.soil

import org.junit.Assert.assertEquals
import org.junit.Test

class SampleCheckTest {
    @Test fun `no test yet means not started, however much was watered`() =
        assertEquals(SampleStatus.NotStarted, SampleCheck.status(hasTest = false, wateringsSince = 9))

    @Test fun `right after a test, two waterings to go`() =
        assertEquals(SampleStatus.Waiting(left = 2), SampleCheck.status(hasTest = true, wateringsSince = 0))

    @Test fun `one watering later, one to go`() =
        assertEquals(SampleStatus.Waiting(left = 1), SampleCheck.status(hasTest = true, wateringsSince = 1))

    @Test fun `every second watering makes it due`() {
        assertEquals(SampleStatus.Due(wateringsSince = 2), SampleCheck.status(hasTest = true, wateringsSince = 2))
        assertEquals(SampleStatus.Due(wateringsSince = 5), SampleCheck.status(hasTest = true, wateringsSince = 5))
    }

    @Test fun `labels say what to do`() {
        assertEquals("Test a sample of your mix to start", SampleCheck.label(SampleStatus.NotStarted))
        assertEquals("Next check after 1 more watering", SampleCheck.label(SampleStatus.Waiting(1)))
        assertEquals("Next check after 2 more waterings", SampleCheck.label(SampleStatus.Waiting(2)))
        assertEquals("Time to check your soil sample · 2 waterings since the last check", SampleCheck.label(SampleStatus.Due(2)))
    }
}
