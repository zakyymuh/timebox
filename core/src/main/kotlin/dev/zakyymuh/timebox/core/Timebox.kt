package dev.zakyymuh.timebox.core

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

enum class BlockStatus { PENDING, RUNNING, DONE, SKIPPED }

/**
 * One stretch of the day: a thing to do and how long it is meant to take.
 *
 * A block has a length, not a start and end time. The day runs back-to-back from
 * whenever it actually begins, because a 09:00 block is a lie by 09:20 on a day that
 * started late, and an app full of overdue blocks before breakfast is an app you stop
 * opening.
 *
 * [startedAt] and [finishedAt] are the only record of time spent. Nothing accumulates
 * elapsed time in memory — a timer dies with the process, and the day does not.
 */
data class Block(
    val id: Long,
    val position: Int,
    val title: String,
    val planned: Duration,
    val startedAt: Instant? = null,
    val finishedAt: Instant? = null,
    val skipped: Boolean = false,
) {
    val status: BlockStatus
        get() = when {
            skipped -> BlockStatus.SKIPPED
            finishedAt != null -> BlockStatus.DONE
            startedAt != null -> BlockStatus.RUNNING
            else -> BlockStatus.PENDING
        }
}

/**
 * A day's plan: what it is for, and the blocks it is made of.
 *
 * [blocks] is ordered by [Block.position]. At most one of them is RUNNING — the
 * invariant lives in [TimeboxEngine], not in any screen, because the UI makes
 * "done, next" a single tap.
 *
 * A block belongs to the day it was created under even if it finishes after midnight.
 */
data class DayTimebox(
    val date: LocalDate,
    val intention: String?,
    val blocks: List<Block>,
)
