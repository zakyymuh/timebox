package dev.zakyymuh.timebox.core.plants

import dev.zakyymuh.timebox.core.collection.DexEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class PlantEventsTest {

    private val today = LocalDate.of(2026, 11, 9)
    private fun d(day: Int, month: Int = 11) = LocalDate.of(2026, month, day)

    private val basil = Plant(1, "Basil", PlantColour.MOSS, Recurrence.EveryDays(3), anchor = d(1))

    private fun dex(name: String, acquired: LocalDate?, photo: String? = null, owned: Boolean = true) =
        DexEntry(id = name.length.toLong(), number = 1, name = name, species = "", owned = owned,
            photo = photo, acquired = acquired, note = "")

    private fun all(plants: List<Plant>, waterings: List<Watering>, entries: List<DexEntry>) =
        PlantEvents.lines(plants, waterings, entries, today)

    @Test
    fun `nothing logged means no line`() {
        assertNull(PlantEvents.random(listOf(basil), emptyList(), emptyList(), today, Random(1)))
    }

    @Test
    fun `the last watering of a plant`() {
        val lines = all(listOf(basil), listOf(Watering(1, 1, d(5))), emptyList())
        assertTrue("You last watered Basil 4 days ago." in lines)
    }

    @Test
    fun `how many times a plant has been watered, once there are two`() {
        assertTrue(all(listOf(basil), listOf(Watering(1, 1, d(5))), emptyList()).none { "times" in it })
        val lines = all(listOf(basil), listOf(Watering(1, 1, d(2)), Watering(2, 1, d(5))), emptyList())
        assertTrue("You've watered Basil 2 times." in lines)
    }

    @Test
    fun `waterings of deleted plants are ignored`() {
        assertEquals(emptyList<String>(), all(listOf(basil), listOf(Watering(1, 99, d(5))), emptyList()))
    }

    @Test
    fun `the oldest plant in the collection, and when each one arrived`() {
        val lines = all(emptyList(), emptyList(), listOf(dex("Monstera", d(9, 8)), dex("Aloe", d(1))))
        assertTrue("Your oldest plant is Monstera, about 3 months with you." in lines)
        assertTrue("You got Aloe 8 days ago." in lines)
    }

    @Test
    fun `wishlist plants and plants without a date are left out`() {
        val lines = all(emptyList(), emptyList(), listOf(dex("Fig", d(1), owned = false), dex("Jade", null)))
        assertEquals(emptyList<String>(), lines)
    }

    @Test
    fun `the number of photos in the collection`() {
        assertTrue("Your collection has 1 photo." in all(emptyList(), emptyList(), listOf(dex("Jade", null, photo = "j.jpg"))))
        val two = listOf(dex("Jade", null, photo = "j.jpg"), dex("Aloe", null, photo = "a.jpg"))
        assertTrue("Your collection has 2 photos." in all(emptyList(), emptyList(), two))
    }

    @Test
    fun `random picks one of the lines, the same for the same seed`() {
        val waterings = listOf(Watering(1, 1, d(2)), Watering(2, 1, d(5)))
        val lines = all(listOf(basil), waterings, emptyList())
        val picked = PlantEvents.random(listOf(basil), waterings, emptyList(), today, Random(7))
        assertTrue(picked in lines)
        assertEquals(picked, PlantEvents.random(listOf(basil), waterings, emptyList(), today, Random(7)))
    }
}
