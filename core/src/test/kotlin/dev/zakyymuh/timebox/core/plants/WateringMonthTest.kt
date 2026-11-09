package dev.zakyymuh.timebox.core.plants

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WateringMonthTest {

    private val today = LocalDate.of(2026, 11, 9)
    private fun d(day: Int, month: Int = 11) = LocalDate.of(2026, month, day)

    private val basil = Plant(1, "Basil", PlantColour.MOSS, Recurrence.EveryDays(3), anchor = d(1, 10))
    private val aloe = Plant(2, "Aloe", PlantColour.OCHRE, Recurrence.EveryDays(7), anchor = d(1, 10))
    private val fern = Plant(3, "Fern", PlantColour.DUSK, Recurrence.EveryDays(7), anchor = d(1, 10))

    private fun w(id: Long, plant: Long, date: LocalDate) = Watering(id, plant, date)

    private val waterings = listOf(
        w(1, 1, d(2)), w(2, 1, d(5)), w(3, 1, d(8)), w(4, 2, d(8)),
        w(5, 1, d(20, 10)), w(6, 2, d(28, 10)),
        w(7, 99, d(9)), // a deleted plant still counts toward the total
    )

    private val month = WateringMonth.of(listOf(basil, aloe, fern), waterings, today)

    @Test
    fun `counts this month's waterings and last month's`() {
        assertEquals(5, month.total)
        assertEquals(2, month.lastMonth)
    }

    @Test
    fun `per plant, most watered first, leaving out plants not watered`() {
        assertEquals(listOf("Basil" to 3, "Aloe" to 1), month.perPlant.map { it.first.name to it.second })
    }

    @Test
    fun `the comparison line`() {
        assertEquals("3 more than October", WateringMonth.compare(month, today))
        assertEquals("1 fewer than October", WateringMonth.compare(month.copy(total = 1), today))
        assertEquals("Same as October", WateringMonth.compare(month.copy(total = 2), today))
        assertEquals("None in October", WateringMonth.compare(month.copy(lastMonth = 0), today))
    }

    @Test
    fun `the headline is singular for one`() {
        assertEquals("5 waterings", WateringMonth.headline(month))
        assertEquals("1 watering", WateringMonth.headline(month.copy(total = 1)))
    }
}
