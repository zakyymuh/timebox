package dev.zakyymuh.timebox.core.plants

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeekStripTest {

    private val today = LocalDate.of(2026, 11, 11)
    private fun d(day: Int) = LocalDate.of(2026, 11, day)

    private fun plant(id: Long, name: String, every: Int, anchor: LocalDate, snooze: LocalDate? = null) = Plant(
        id = id, name = name, colour = PlantColour.MOSS,
        recurrence = Recurrence.EveryDays(every), anchor = anchor, snoozedUntil = snooze,
    )

    private fun strip(plants: List<Plant>, waterings: List<Watering> = emptyList()) =
        WeekStrip.of(plants, waterings, today)

    private fun names(day: StripDay) = day.plants.map { it.plant.name to it.mark }

    @Test
    fun `seven days with today in the middle, in date order`() {
        assertEquals((8..14).map(::d), strip(emptyList()).map { it.date })
    }

    @Test
    fun `past days show the waterings that were logged`() {
        val fern = plant(1, "Fern", every = 30, anchor = d(1))
        val days = strip(listOf(fern), listOf(Watering(1, 1, d(9))))
        assertEquals(listOf("Fern" to DayMark.Watered), names(days[1]))
        assertEquals(emptyList<Pair<String, DayMark>>(), names(days[0]))
    }

    @Test
    fun `waterings older than three days, or for deleted plants, are left out`() {
        val fern = plant(1, "Fern", every = 30, anchor = d(1))
        val days = strip(listOf(fern), listOf(Watering(1, 1, d(7)), Watering(2, 99, d(9))))
        assertEquals(List(7) { emptyList<Pair<String, DayMark>>() }, days.map(::names))
    }

    @Test
    fun `today shows overdue plants first, then plants due today`() {
        // Only one-time plants can be late: a repeating one skips a missed day.
        val late = Plant(1, "Aloe", PlantColour.MOSS, Recurrence.Once(d(9)), anchor = d(2))
        val now = plant(2, "Basil", every = 7, anchor = d(4)) // due the 11th
        assertEquals(
            listOf("Aloe" to DayMark.Due(2), "Basil" to DayMark.Due(0)),
            names(strip(listOf(now, late))[3]),
        )
    }

    @Test
    fun `a plant watered today shows as watered, not due`() {
        val basil = plant(1, "Basil", every = 7, anchor = d(4))
        assertEquals(
            listOf("Basil" to DayMark.Watered),
            names(strip(listOf(basil), listOf(Watering(1, 1, today)))[3]),
        )
    }

    @Test
    fun `a repeating plant appears on every coming day it falls on`() {
        val cress = plant(1, "Cress", every = 1, anchor = d(10)) // due today, then daily
        val days = strip(listOf(cress))
        assertEquals(listOf("Cress" to DayMark.Due(0)), names(days[3]))
        (4..6).forEach { assertEquals(listOf("Cress" to DayMark.Coming), names(days[it])) }
    }

    @Test
    fun `a repeating plant that missed its days is back on the beat, not late`() {
        val aloe = plant(1, "Aloe", every = 2, anchor = d(5)) // due the 7th and 9th, both missed
        val days = strip(listOf(aloe))
        assertEquals(listOf("Aloe" to DayMark.Due(0)), names(days[3]))
        assertEquals(emptyList<Pair<String, DayMark>>(), names(days[4]))
        assertEquals(listOf("Aloe" to DayMark.Coming), names(days[5]))
        // Missed the 10th with an every-3 plant: next is the 13th, nothing today.
        val fig = plant(2, "Fig", every = 3, anchor = d(7))
        val figDays = strip(listOf(fig))
        assertEquals(emptyList<Pair<String, DayMark>>(), names(figDays[3]))
        assertEquals(listOf("Fig" to DayMark.Coming), names(figDays[5]))
    }

    @Test
    fun `a snoozed plant first shows on its snooze day`() {
        val fig = plant(1, "Fig", every = 7, anchor = d(3), snooze = d(13)) // due the 10th, snoozed
        val days = strip(listOf(fig))
        assertEquals(emptyList<Pair<String, DayMark>>(), names(days[3]))
        assertEquals(listOf("Fig" to DayMark.Coming), names(days[5]))
    }

    @Test
    fun `a one-time plant shows once, and not at all once done`() {
        val seed = Plant(1, "Seed", PlantColour.OCHRE, Recurrence.Once(d(13)), anchor = d(1))
        assertEquals(listOf(0, 0, 0, 0, 0, 1, 0), strip(listOf(seed)).map { it.plants.size })
        val done = strip(listOf(seed), listOf(Watering(1, 1, d(5))))
        assertEquals(List(7) { 0 }, done.map { it.plants.size })
    }

    @Test
    fun `coming plants are ordered by name`() {
        val b = plant(1, "Bonsai", every = 7, anchor = d(5)) // due the 12th
        val a = plant(2, "Agave", every = 7, anchor = d(5))
        assertEquals(listOf("Agave", "Bonsai"), strip(listOf(b, a))[4].plants.map { it.plant.name })
    }

    @Test
    fun `each mark has a short label for the day's list`() {
        assertEquals("watered", PlantCopy.mark(DayMark.Watered))
        assertEquals("due today", PlantCopy.mark(DayMark.Due(0)))
        assertEquals("1 day late", PlantCopy.mark(DayMark.Due(1)))
        assertEquals("3 days late", PlantCopy.mark(DayMark.Due(3)))
        assertEquals("due", PlantCopy.mark(DayMark.Coming))
    }

    @Test
    fun `an empty day says so, in the past and ahead`() {
        assertEquals("Nothing watered", PlantCopy.emptyDay(d(9), today))
        assertEquals("Nothing due", PlantCopy.emptyDay(today, today))
        assertEquals("Nothing due", PlantCopy.emptyDay(d(13), today))
    }

    @Test
    fun `a day's border - watered beats to do, and an empty day has none`() {
        val basil = plant(1, "Basil", every = 7, anchor = d(4)) // due today
        val fern = plant(2, "Fern", every = 30, anchor = d(1))
        val days = strip(listOf(basil, fern), listOf(Watering(1, 2, d(9))))
        assertEquals(DayBorder.Watered, days[1].border)
        assertEquals(DayBorder.ToDo, days[3].border)
        assertEquals(DayBorder.None, days[0].border)
        val both = StripDay(today, listOf(DayPlant(basil, DayMark.Watered), DayPlant(basil, DayMark.Due(0))))
        assertEquals(DayBorder.Watered, both.border)
        assertEquals(DayBorder.ToDo, StripDay(d(13), listOf(DayPlant(basil, DayMark.Coming))).border)
    }
}
