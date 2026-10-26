package dev.zakyymuh.timebox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ItemsTest {

    private val now = Instant.parse("2026-10-27T09:14:00Z")
    private val today = LocalDate.of(2026, 10, 27)
    private val yesterday = LocalDate.of(2026, 10, 26)

    private fun item(id: Long, pos: Int, text: String = "item $id", done: Instant? = null) =
        TodoItem(id, pos, text, done)

    private fun day(vararg items: TodoItem, note: String? = "Ship the auth refactor") =
        DayList(today, note, items.toList())

    private fun DayList.item(id: Long) = items.single { it.id == id }

    @Test
    fun `an item with no done instant is not done`() {
        assertFalse(item(1, 0).isDone)
    }

    @Test
    fun `toggle ticks an item off and records when`() {
        val d = Items.toggle(day(item(1, 0), item(2, 1)), 1, now)
        assertTrue(d.item(1).isDone)
        assertEquals(now, d.item(1).doneAt)
    }

    @Test
    fun `toggle un-ticks an item that was already done`() {
        val d = Items.toggle(day(item(1, 0, done = now)), 1, now.plusSeconds(60))
        assertFalse(d.item(1).isDone)
        assertNull(d.item(1).doneAt)
    }

    @Test
    fun `toggle leaves the other items alone`() {
        val d = Items.toggle(day(item(1, 0), item(2, 1)), 1, now)
        assertFalse(d.item(2).isDone)
    }

    @Test
    fun `add appends an item at the end and trims its text`() {
        val d = Items.add(day(item(1, 0)), "  Reply to Rina  ")
        assertEquals(2, d.items.size)
        assertEquals("Reply to Rina", d.items.last().text)
        assertEquals(1, d.items.last().position)
    }

    @Test
    fun `remove deletes an item and closes the gap`() {
        val d = Items.remove(day(item(1, 0), item(2, 1), item(3, 2)), 2)
        assertEquals(listOf(1L, 3L), d.items.map { it.id })
        assertEquals(listOf(0, 1), d.items.map { it.position })
    }

    @Test
    fun `rename replaces the text and trims it`() {
        val d = Items.rename(day(item(1, 0)), 1, "  Groceries ")
        assertEquals("Groceries", d.item(1).text)
    }

    @Test
    fun `reorder moves an item and renumbers from zero`() {
        val out = Items.reorder(listOf(item(1, 0), item(2, 1), item(3, 2)), 0, 2)
        assertEquals(listOf(2L, 3L, 1L), out.map { it.id })
        assertEquals(listOf(0, 1, 2), out.map { it.position })
    }

    @Test
    fun `reorder to the same index changes nothing`() {
        val input = listOf(item(1, 0), item(2, 1))
        assertEquals(input, Items.reorder(input, 1, 1))
    }

    @Test
    fun `renumber closes gaps left by a deletion`() {
        val out = Items.renumber(listOf(item(1, 0), item(3, 7), item(4, 9)))
        assertEquals(listOf(0, 1, 2), out.map { it.position })
    }

    @Test
    fun `templateFrom drops every done instant so the new day starts fresh`() {
        val source = DayList(
            yesterday, "yesterday's note",
            listOf(item(1, 0, "Workout", done = now), item(2, 1, "Thesis", done = now)),
        )
        val out = Items.templateFrom(source, today)
        assertTrue(out.items.none { it.isDone })
    }

    @Test
    fun `templateFrom keeps the texts and their order`() {
        val source = DayList(yesterday, "x", listOf(item(1, 0, "Workout"), item(2, 1, "Thesis")))
        val out = Items.templateFrom(source, today)
        assertEquals(listOf("Workout", "Thesis"), out.items.map { it.text })
        assertEquals(listOf(0, 1), out.items.map { it.position })
    }

    @Test
    fun `templateFrom sets the new date and drops the old note`() {
        val source = DayList(yesterday, "yesterday's note", listOf(item(1, 0)))
        val out = Items.templateFrom(source, today)
        assertEquals(today, out.date)
        assertNull(out.note)
    }

    @Test
    fun `templateFrom clears ids so the copies save as new rows`() {
        val source = DayList(yesterday, "x", listOf(item(7, 0), item(9, 1)))
        assertTrue(Items.templateFrom(source, today).items.all { it.id == 0L })
    }
}
