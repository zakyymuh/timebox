package dev.zakyymuh.timebox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class WidgetViewTest {

    private val now = Instant.parse("2026-10-27T09:14:00Z")
    private val date = LocalDate.of(2026, 10, 27)

    private fun item(id: Long, text: String, done: Boolean = false) =
        TodoItem(id, id.toInt(), text, if (done) now else null)

    private fun day(vararg items: TodoItem, note: String? = "Ship the auth refactor") =
        DayList(date, note, items.toList())

    @Test
    fun `a day with no items needs a list`() {
        assertTrue(WidgetViews.of(day(), maxVisible = 4) is WidgetView.NeedsList)
    }

    @Test
    fun `a null day needs a list rather than crashing`() {
        assertTrue(WidgetViews.of(null, maxVisible = 4) is WidgetView.NeedsList)
    }

    @Test
    fun `unfinished items come before finished ones`() {
        val v = WidgetViews.of(
            day(item(1, "done thing", done = true), item(2, "todo thing")),
            maxVisible = 4,
        ) as WidgetView.HasList
        assertEquals(listOf("todo thing", "done thing"), v.visible.map { it.text })
    }

    @Test
    fun `visible is capped at maxVisible and the rest are counted`() {
        val v = WidgetViews.of(
            day(item(1, "a"), item(2, "b"), item(3, "c"), item(4, "d"), item(5, "e")),
            maxVisible = 3,
        ) as WidgetView.HasList
        assertEquals(listOf("a", "b", "c"), v.visible.map { it.text })
        assertEquals(2, v.hidden)
    }

    @Test
    fun `hidden is zero when everything fits`() {
        val v = WidgetViews.of(day(item(1, "a")), maxVisible = 4) as WidgetView.HasList
        assertEquals(0, v.hidden)
    }

    @Test
    fun `the note and the summary come through`() {
        val v = WidgetViews.of(
            day(item(1, "a", done = true), item(2, "b")),
            maxVisible = 4,
        ) as WidgetView.HasList
        assertEquals("Ship the auth refactor", v.note)
        assertEquals(1, v.summary.done)
        assertEquals(2, v.summary.total)
    }

    @Test
    fun `an all-done day still shows its items`() {
        val v = WidgetViews.of(
            day(item(1, "a", done = true), item(2, "b", done = true)),
            maxVisible = 4,
        ) as WidgetView.HasList
        assertEquals(2, v.visible.size)
        assertTrue(v.summary.allDone)
    }
}
