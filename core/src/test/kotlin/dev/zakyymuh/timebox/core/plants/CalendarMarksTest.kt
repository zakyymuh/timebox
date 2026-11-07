package dev.zakyymuh.timebox.core.plants

import dev.zakyymuh.timebox.core.plants.MarkKind.PROJECTED
import dev.zakyymuh.timebox.core.plants.MarkKind.WATERED
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarMarksTest {

    private val today = LocalDate.of(2026, 11, 7)
    private val november = YearMonth.of(2026, 11)
    private fun d(day: Int, month: Int = 11) = LocalDate.of(2026, month, day)

    private fun plant(every: Int, anchor: LocalDate, id: Long = 1) =
        Plant(id, "P$id", PlantColour.MOSS, Recurrence.EveryDays(every), anchor)

    /** date → kinds, for one plant, so assertions read as a calendar. */
    private fun kinds(marks: Map<LocalDate, List<Mark>>) =
        marks.mapValues { (_, list) -> list.map { it.kind } }

    @Test
    fun `past waterings are solid and never projected`() {
        val p = plant(7, d(1))
        val marks = CalendarMarks.forMonth(november, listOf(p), listOf(Watering(1, 1, d(1))), today)
        assertEquals(mapOf(d(1) to listOf(WATERED)), kinds(marks).filterKeys { it < today })
    }

    @Test
    fun `projections repeat every interval to the end of the month`() {
        val marks = CalendarMarks.forMonth(november, listOf(plant(7, d(1))), emptyList(), today)
        assertEquals(listOf(d(8), d(15), d(22), d(29)), marks.keys.sorted())
        assertEquals(setOf(listOf(PROJECTED)), kinds(marks).values.toSet())
    }

    @Test
    fun `an overdue plant is projected on today, then every interval from today`() {
        // Every 3, anchor 1 Nov → due 4 Nov, three days late.
        val marks = CalendarMarks.forMonth(november, listOf(plant(3, d(1))), emptyList(), today)
        assertEquals(listOf(7, 10, 13, 16, 19, 22, 25, 28).map { d(it) }, marks.keys.sorted())
    }

    @Test
    fun `watering today replaces today's projection with a solid mark`() {
        val marks = CalendarMarks.forMonth(november, listOf(plant(3, d(1))), listOf(Watering(1, 1, today)), today)
        assertEquals(listOf(WATERED), kinds(marks)[today])
        assertEquals(listOf(PROJECTED), kinds(marks)[d(10)])
    }

    @Test
    fun `a month entirely in the past has only solid marks`() {
        val p = plant(7, d(1, 10))
        val marks = CalendarMarks.forMonth(YearMonth.of(2026, 10), listOf(p), listOf(Watering(1, 1, d(20, 10))), today)
        assertEquals(mapOf(d(20, 10) to listOf(WATERED)), kinds(marks))
    }

    @Test
    fun `a future month continues the projection chain`() {
        val marks = CalendarMarks.forMonth(YearMonth.of(2026, 12), listOf(plant(7, d(1))), emptyList(), today)
        assertEquals(listOf(6, 13, 20, 27).map { d(it, 12) }, marks.keys.sorted())
    }

    @Test
    fun `a done one-time plant shows only its watering`() {
        val once = Plant(1, "Cactus", PlantColour.OCHRE, Recurrence.Once(d(10)), today)
        val marks = CalendarMarks.forMonth(november, listOf(once), listOf(Watering(1, 1, d(5))), today)
        assertEquals(mapOf(d(5) to listOf(WATERED)), kinds(marks))
    }

    @Test
    fun `an unwatered one-time plant is projected once, on today if its date has passed`() {
        val late = Plant(1, "Cactus", PlantColour.OCHRE, Recurrence.Once(d(4)), d(4))
        assertEquals(mapOf(today to listOf(PROJECTED)), kinds(CalendarMarks.forMonth(november, listOf(late), emptyList(), today)))
    }

    @Test
    fun `marks on one day are ordered by plant id and carry the plant's colour`() {
        val a = plant(7, d(1), id = 2).copy(colour = PlantColour.PLUM)
        val b = plant(7, d(1), id = 1)
        val day8 = CalendarMarks.forMonth(november, listOf(a, b), emptyList(), today)[d(8)]!!
        assertEquals(listOf(1L, 2L), day8.map { it.plantId })
        assertEquals(PlantColour.PLUM, day8[1].colour)
    }
}
