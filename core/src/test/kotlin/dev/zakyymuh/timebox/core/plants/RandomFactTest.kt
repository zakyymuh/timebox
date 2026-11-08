package dev.zakyymuh.timebox.core.plants

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class RandomFactTest {
    private val every = Encyclopedia.entries.flatMap { e -> e.funFacts.map { FunFact(it, e) } }.toSet()

    @Test
    fun `the same seed gives the same fact`() {
        assertEquals(FunFacts.random(Random(7)), FunFacts.random(Random(7)))
    }

    @Test
    fun `every fact in the encyclopedia can come up`() {
        val seen = (0 until 2_000).map { FunFacts.random(Random(it)) }.toSet()
        assertEquals(every, seen)
    }
}
