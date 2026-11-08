package dev.zakyymuh.timebox.core

import org.junit.Assert.assertEquals
import org.junit.Test

class HabitCheerTest {
    @Test fun `nothing planned asks you to plan`() = assertEquals("Plan your day", HabitCheer.of(DaySummary(0, 0)))
    @Test fun `nothing done yet is a gentle start`() = assertEquals("Let's get started", HabitCheer.of(DaySummary(0, 5)))
    @Test fun `under half is a good start`() = assertEquals("Good start, keep going", HabitCheer.of(DaySummary(2, 5)))
    @Test fun `half or more is over halfway`() = assertEquals("Over halfway!", HabitCheer.of(DaySummary(3, 6)))
    @Test fun `all done celebrates`() = assertEquals("All done, well done 🎉", HabitCheer.of(DaySummary(5, 5)))
}
