package dev.zakyymuh.timebox.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class WidgetStateBuilderTest {

    private val now = Instant.parse("2026-10-26T09:14:00Z")
    private val plan = DayPlan(LocalDate.of(2026, 10, 26), "Ship the auth refactor", now)
    private val recent = listOf("Ship the auth refactor", "Thesis chapter 3", "Rest day, no screens", "Inbox zero")

    @Test
    fun `with no note it offers the recent intentions as suggestions`() {
        val s = WidgetStateBuilder().build(null, recent, 0, true, 3) as WidgetState.NeedsNote
        assertEquals(listOf("Ship the auth refactor", "Thesis chapter 3", "Rest day, no screens"), s.suggestions)
    }

    @Test
    fun `it offers at most maxSuggestions`() {
        val s = WidgetStateBuilder().build(null, recent, 0, true, 1) as WidgetState.NeedsNote
        assertEquals(listOf("Ship the auth refactor"), s.suggestions)
    }

    @Test
    fun `with no note and no history it offers an empty suggestion list`() {
        val s = WidgetStateBuilder().build(null, emptyList(), 0, true, 3) as WidgetState.NeedsNote
        assertEquals(emptyList<String>(), s.suggestions)
    }

    @Test
    fun `with a note it shows the intention, stop count and watching flag`() {
        val s = WidgetStateBuilder().build(plan, recent, 4, true, 3) as WidgetState.HasNote
        assertEquals("Ship the auth refactor", s.intention)
        assertEquals(4, s.stopsToday)
        assertEquals(true, s.watching)
    }

    @Test
    fun `a note from yesterday is not a note for today`() {
        // After midnight the repository hands over a null plan, and the widget must go
        // back to asking rather than showing yesterday's intention.
        val s = WidgetStateBuilder().build(null, recent, 0, true, 3)
        assertEquals(WidgetState.NeedsNote(recent.take(3)), s)
    }

    @Test
    fun `it reports not watching so the widget can warn`() {
        val s = WidgetStateBuilder().build(plan, recent, 4, false, 3) as WidgetState.HasNote
        assertEquals(false, s.watching)
    }
}
