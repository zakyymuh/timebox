package dev.zakyymuh.timebox.core.plants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlantNoticesTest {

    private val today = LocalDate.of(2026, 11, 9)

    private fun every(id: Long, name: String, days: Int, anchor: LocalDate, snooze: LocalDate? = null) =
        Plant(id, name, PlantColour.MOSS, Recurrence.EveryDays(days), anchor = anchor, snoozedUntil = snooze)

    private fun evening(vararg plants: Plant) =
        PlantNotices.evening(PlantsOverview.of(plants.toList(), emptyList(), today))

    @Test
    fun `one notice per plant due, late ones first`() {
        val basil = every(1, "Basil", 3, today.minusDays(3)) // due today
        val aloe = every(2, "Aloe", 3, today.minusDays(5)) // 2 days late
        val fern = every(3, "Fern", 3, today) // not due
        assertEquals(
            listOf(2L to "Aloe needs water · 2 days late", 1L to "Basil needs water"),
            evening(basil, aloe, fern).map { it.plantId to it.title },
        )
    }

    @Test
    fun `a one-time plant on its day gets a notice`() {
        val seed = Plant(4, "Seed", PlantColour.OCHRE, Recurrence.Once(today), anchor = today.minusDays(1))
        assertEquals(listOf("Seed needs water"), evening(seed).map { it.title })
    }

    @Test
    fun `a snoozed plant gets no notice`() {
        val fig = every(5, "Fig", 3, today.minusDays(3), snooze = today.plusDays(1))
        assertEquals(emptyList<PlantNotices.Notice>(), evening(fig))
    }

    @Test
    fun `ids and request codes never clash, between plants or with the app's fixed ones`() {
        val fixed = setOf(2, 3, 200, 201, 202, 203)
        val all = (1L..50L).flatMap { id ->
            with(PlantNotices) { listOf(notificationId(id), openCode(id), waterCode(id), checkCode(id)) }
        }
        assertEquals(all.size, all.toSet().size)
        assertTrue(all.none { it in fixed })
    }
}
