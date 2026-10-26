package dev.zakyymuh.timebox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class GateDeciderTest {

    private val now = Instant.parse("2026-10-26T09:14:00Z")
    private val plan = DayPlan(LocalDate.of(2026, 10, 26), "Ship the auth refactor", now)

    private fun ctx(
        pkg: String = "com.instagram.android",
        blocked: Set<String> = setOf("com.instagram.android"),
        pass: Pass? = null,
        todaysPlan: DayPlan? = plan,
        stops: Int = 3,
    ) = GateContext(pkg, blocked, pass, todaysPlan, stops, now)

    @Test
    fun `allows an app that is not on the off-plan list`() {
        assertEquals(GateDecision.Allow, GateDecider().decide(ctx(pkg = "com.android.calculator2")))
    }

    @Test
    fun `blocks an off-plan app and carries today's intention`() {
        val d = GateDecider().decide(ctx()) as GateDecision.Block
        assertEquals(BlockReason.OFF_PLAN, d.reason)
        assertEquals("Ship the auth refactor", d.intention)
        assertEquals(3, d.stopsToday)
    }

    @Test
    fun `allows an off-plan app while its pass is unexpired`() {
        val pass = Pass(
            1, "com.instagram.android", now.minusSeconds(60),
            now.plusSeconds(240), "need the DM from Rina about the venue",
        )
        assertEquals(GateDecision.Allow, GateDecider().decide(ctx(pass = pass)))
    }

    @Test
    fun `blocks again once the pass has expired`() {
        val pass = Pass(
            1, "com.instagram.android", now.minusSeconds(600),
            now.minusSeconds(300), "need the DM from Rina about the venue",
        )
        assertTrue(GateDecider().decide(ctx(pass = pass)) is GateDecision.Block)
    }

    @Test
    fun `treats the instant of expiry as expired`() {
        val pass = Pass(1, "com.instagram.android", now.minusSeconds(300), now, "reason that is long enough")
        assertTrue(GateDecider().decide(ctx(pass = pass)) is GateDecision.Block)
    }

    @Test
    fun `blocks with NO_PLAN_YET when no note has been written today`() {
        val d = GateDecider().decide(ctx(todaysPlan = null)) as GateDecision.Block
        assertEquals(BlockReason.NO_PLAN_YET, d.reason)
        assertNull(d.intention)
    }

    @Test
    fun `an unexpired pass wins over a missing note`() {
        val pass = Pass(1, "com.instagram.android", now, now.plusSeconds(240), "reason that is long enough")
        assertEquals(GateDecision.Allow, GateDecider().decide(ctx(todaysPlan = null, pass = pass)))
    }

    @Test
    fun `an app that is not off-plan is allowed even with no note`() {
        assertEquals(
            GateDecision.Allow,
            GateDecider().decide(ctx(pkg = "com.android.calculator2", todaysPlan = null)),
        )
    }
}
