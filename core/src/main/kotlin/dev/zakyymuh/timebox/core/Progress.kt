package dev.zakyymuh.timebox.core

import java.time.Duration
import java.time.Instant

/**
 * How a block is doing against its planned length.
 *
 * Overrun is reported, not corrected. Deciding to carry on past the planned time is a
 * legitimate choice and the app does not overrule it — it just says so.
 */
data class BlockProgress(
    val elapsed: Duration,
    /** The unused part of the planned length, floored at zero. */
    val remaining: Duration,
    /** How far past the planned length, or zero. */
    val overrunBy: Duration,
) {
    val isOverrun: Boolean get() = overrunBy > Duration.ZERO
}

object Progress {

    /** Null when the block has not started: there is nothing yet to report. */
    fun of(block: Block, now: Instant): BlockProgress? {
        val started = block.startedAt ?: return null

        // A finished block is measured to its finish, so it stops growing once done.
        val until = block.finishedAt ?: now
        val elapsed = Duration.between(started, until)

        val remaining = block.planned.minus(elapsed)
        val overrun = elapsed.minus(block.planned)

        return BlockProgress(
            elapsed = elapsed,
            remaining = if (remaining.isNegative) Duration.ZERO else remaining,
            overrunBy = if (overrun.isNegative) Duration.ZERO else overrun,
        )
    }
}
