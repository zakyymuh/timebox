package dev.zakyymuh.timebox.core

import java.time.Instant

/**
 * Every transition a day can make. Pure: each call returns a new [DayTimebox] and
 * nothing is mutated.
 *
 * The invariant this exists to hold is that **at most one block is RUNNING**. It cannot
 * live in a screen, because the UI makes "done, next" a single tap and the widget and a
 * notification action can both do the same thing from outside the app. So starting any
 * block finishes whatever was running, here, once.
 */
class TimeboxEngine {

    fun running(day: DayTimebox): Block? =
        day.blocks.firstOrNull { it.status == BlockStatus.RUNNING }

    /**
     * The next block still waiting to be done.
     *
     * With [afterPosition] given, only blocks later in the day count — "next" means
     * forward. Without it, the earliest pending block, which is where a day that has
     * not started yet begins.
     */
    fun nextPending(day: DayTimebox, afterPosition: Int? = null): Block? =
        day.blocks
            .filter { it.status == BlockStatus.PENDING }
            .filter { afterPosition == null || it.position > afterPosition }
            .minByOrNull { it.position }

    /**
     * Starts [blockId], finishing any running block first.
     *
     * Starting a block that is already DONE re-opens it — clearing its finish time —
     * so that redoing something is possible without a separate command.
     */
    fun start(day: DayTimebox, blockId: Long, now: Instant): DayTimebox {
        if (running(day)?.id == blockId) return day

        val closed = finishRunning(day, now)
        return closed.map(blockId) {
            it.copy(startedAt = now, finishedAt = null, skipped = false)
        }
    }

    fun finishRunning(day: DayTimebox, now: Instant): DayTimebox {
        val current = running(day) ?: return day
        return day.map(current.id) { it.copy(finishedAt = now) }
    }

    /**
     * Finish what is running and begin the next thing. The everyday action.
     *
     * "Next" is strictly forward from the block that was running: finishing the middle
     * block moves on to the one after it, never back to an earlier block that was
     * never started. Reaching the end therefore starts nothing rather than wrapping
     * around. When nothing was running, the day begins at its earliest pending block.
     */
    fun advance(day: DayTimebox, now: Instant): DayTimebox {
        val current = running(day)
        val closed = finishRunning(day, now)
        val next = nextPending(closed, afterPosition = current?.position) ?: return closed
        return closed.map(next.id) { it.copy(startedAt = now, finishedAt = null) }
    }

    /**
     * Abandons [blockId]. A plan dropped on purpose should stay distinguishable from
     * one completed and from one never reached, so this is its own status rather than
     * a deletion. Skipping the running block also closes it.
     */
    fun skip(day: DayTimebox, blockId: Long, now: Instant): DayTimebox {
        val isRunning = running(day)?.id == blockId
        return day.map(blockId) {
            it.copy(skipped = true, finishedAt = if (isRunning) now else it.finishedAt)
        }
    }

    private fun DayTimebox.map(blockId: Long, change: (Block) -> Block): DayTimebox =
        copy(blocks = blocks.map { if (it.id == blockId) change(it) else it })
}
