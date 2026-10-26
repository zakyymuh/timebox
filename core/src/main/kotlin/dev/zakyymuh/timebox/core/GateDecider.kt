package dev.zakyymuh.timebox.core

import java.time.Instant

/** Everything the gate needs to know about one app coming to the foreground. */
data class GateContext(
    val packageName: String,
    val blockedPackages: Set<String>,
    /** The most recent pass for [packageName], expired or not, or null if there is none. */
    val activePass: Pass?,
    val todaysPlan: DayPlan?,
    val stopsToday: Int,
    val now: Instant,
)

enum class BlockReason {
    /** The app is off-plan and today's intention says otherwise. */
    OFF_PLAN,

    /** No intention has been written for today yet, so nothing has been agreed. */
    NO_PLAN_YET,
}

sealed interface GateDecision {
    data object Allow : GateDecision

    data class Block(
        val reason: BlockReason,
        /** Today's intention, or null when [reason] is [BlockReason.NO_PLAN_YET]. */
        val intention: String?,
        val stopsToday: Int,
    ) : GateDecision
}

/**
 * The whole rule set, in one place and with no Android in sight.
 *
 * The order of the checks is the rule: an app that is not off-plan is never
 * interrupted, an unexpired pass beats everything else, and a day with no
 * intention written blocks rather than waves through.
 */
class GateDecider {

    fun decide(ctx: GateContext): GateDecision {
        if (ctx.packageName !in ctx.blockedPackages) return GateDecision.Allow

        val pass = ctx.activePass
        if (pass != null && ctx.now < pass.expiresAt) return GateDecision.Allow

        val plan = ctx.todaysPlan
            ?: return GateDecision.Block(BlockReason.NO_PLAN_YET, null, ctx.stopsToday)

        return GateDecision.Block(BlockReason.OFF_PLAN, plan.intention, ctx.stopsToday)
    }
}
