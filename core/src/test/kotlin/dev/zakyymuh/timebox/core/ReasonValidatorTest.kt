package dev.zakyymuh.timebox.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReasonValidatorTest {

    private val p = PassPolicy.DEFAULT

    @Test
    fun `accepts a real sentence`() {
        assertTrue(ReasonValidator().isAcceptable("need the DM from Rina about the venue", p))
    }

    @Test
    fun `rejects a reason under the character minimum`() {
        assertFalse(ReasonValidator().isAcceptable("quick look", p))
    }

    @Test
    fun `rejects whitespace padding`() {
        assertFalse(ReasonValidator().isAcceptable("                      ", p))
    }

    @Test
    fun `rejects a long run of one repeated character`() {
        assertFalse(ReasonValidator().isAcceptable("aaaaaaaaaaaaaaaaaaaa", p))
    }

    @Test
    fun `rejects too few words even when long enough`() {
        assertFalse(ReasonValidator().isAcceptable("aaaaaaaa bbbbbbbb", p))
    }

    @Test
    fun `counts only words of two or more characters`() {
        assertFalse(ReasonValidator().isAcceptable("checking a b c thing", p))
    }

    @Test
    fun `measures length after trimming`() {
        assertFalse(ReasonValidator().isAcceptable("   short why   ", p))
    }
}
