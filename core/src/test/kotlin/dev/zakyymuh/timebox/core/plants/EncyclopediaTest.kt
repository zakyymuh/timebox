package dev.zakyymuh.timebox.core.plants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EncyclopediaTest {

    @Test
    fun `every entry is complete`() {
        assertTrue(Encyclopedia.entries.size >= 4)
        Encyclopedia.entries.forEach { e ->
            assertTrue(e.id, e.name.isNotBlank() && e.summary.isNotBlank() && e.description.isNotBlank())
            assertTrue(e.id, e.sun.isNotBlank() && e.water.isNotBlank())
            assertTrue(e.id, e.funFacts.size >= 4)
            assertTrue(e.id, e.sources.isNotEmpty())
        }
        assertEquals(Encyclopedia.entries.size, Encyclopedia.entries.map { it.id }.toSet().size)
    }

    @Test
    fun `the three plants the owner keeps are there, with their forms`() {
        assertEquals("Greenovia", Encyclopedia.byId("greenovia")?.name)
        assertTrue(Encyclopedia.byId("astrophytum-asterias")!!.varieties.any { "variegat" in it.name.lowercase() })
        assertTrue(Encyclopedia.byId("astrophytum-myriostigma")!!.varieties.any { "nudum" in it.name.lowercase() })
        assertTrue(Encyclopedia.byId("astrophytum-myriostigma")!!.varieties.any { "variegat" in it.name.lowercase() })
    }

    @Test
    fun `a plant's name finds its entry`() {
        assertEquals("greenovia", Encyclopedia.matching("Greenovia")?.id)
        assertEquals("greenovia", Encyclopedia.matching("mountain rose by the window")?.id)
        assertEquals("astrophytum-myriostigma", Encyclopedia.matching("Astrophytum myriostigma nudum")?.id)
        assertEquals("astrophytum-myriostigma", Encyclopedia.matching("my bishop's cap")?.id)
        assertEquals("astrophytum-asterias", Encyclopedia.matching("Astrophytum asterias variegata")?.id)
        assertEquals("astrophytum-asterias", Encyclopedia.matching("Super Kabuto")?.id)
        assertNull(Encyclopedia.matching("Basil"))
    }

    @Test
    fun `the fact is the same all day and changes the next day`() {
        val day = LocalDate.of(2026, 11, 8)
        assertEquals(FunFacts.forDay(day, emptyList()), FunFacts.forDay(day, emptyList()))
        assertNotEquals(FunFacts.forDay(day, emptyList()), FunFacts.forDay(day.plusDays(1), emptyList()))
    }

    @Test
    fun `facts about plants the owner has come first`() {
        repeat(30) { i ->
            val fact = FunFacts.forDay(LocalDate.of(2026, 11, 1).plusDays(i.toLong()), listOf("Greenovia", "Basil"))
            assertEquals("greenovia", fact.entry.id)
        }
    }

    @Test
    fun `with no matching plants every entry takes a turn`() {
        val days = Encyclopedia.entries.sumOf { it.funFacts.size }
        val seen = (0 until days).map { FunFacts.forDay(LocalDate.of(2026, 1, 1).plusDays(it.toLong()), listOf("Basil")).entry.id }.toSet()
        assertTrue(seen.containsAll(listOf("greenovia", "astrophytum-asterias", "astrophytum-myriostigma")))
    }

    @Test
    fun `the new plants and guides are all there`() {
        listOf(
            "sansevieria", "cacti-succulents", "pseudolithos", "echeveria", "gymnocalycium-mihanovichii",
            "gymnocalycium-lb-vos", "coryphantha", "astrophytum-kikko", "mammillaria", "kaktus-koboi",
            "grafting", "grafting-dragon-fruit", "soil-mix", "seed-sowing",
        ).forEach { assertTrue(it, Encyclopedia.byId(it) != null) }
    }

    @Test
    fun `sansevieria has its Indonesian name and many varieties`() {
        val sanse = Encyclopedia.byId("sansevieria")!!
        assertTrue("lidah mertua" in sanse.otherNames.lowercase())
        assertTrue(sanse.varieties.size >= 6)
    }

    @Test
    fun `the owner's spellings find the right entry`() {
        mapOf(
            "Sansiveira laurentii" to "sansevieria",
            "lidah mertua" to "sansevieria",
            "Mamilaria elongata" to "mammillaria",
            "Coryphanta elephantidens" to "coryphantha",
            "Echievera lola" to "echeveria",
            "Pseudolitos migiurtinus" to "pseudolithos",
            "Gymno LB2178" to "gymnocalycium-lb-vos",
            "Gymno VoS 01-014" to "gymnocalycium-lb-vos",
            "Gymno miha hibotan" to "gymnocalycium-mihanovichii",
            "Kaktus koboi" to "kaktus-koboi",
            "Astrophytum asterias kikko" to "astrophytum-kikko",
        ).forEach { (name, id) -> assertEquals(name, id, Encyclopedia.matching(name)?.id) }
    }
}
