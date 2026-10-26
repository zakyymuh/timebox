package dev.zakyymuh.timebox.core

import java.time.Duration
import java.time.Instant

/**
 * The day at a glance. [plannedTotal] against [actualTotal] is the number that teaches
 * you your estimates are wrong, which is most of the value of keeping the record.
 */
data class DaySummary(
    val plannedTotal: Duration,
    val actualTotal: Duration,
    val done: Int,
    val skipped: Int,
    val total: Int,
)

object DaySummaries {

    fun summarise(day: DayTimebox, now: Instant): DaySummary {
        var actual = Duration.ZERO
        var done = 0
        var skipped = 0

        for (block in day.blocks) {
            when (block.status) {
                // Time spent on something abandoned is not time spent on the plan.
                BlockStatus.SKIPPED -> skipped++
                BlockStatus.DONE -> {
                    done++
                    actual += Progress.of(block, now)?.elapsed ?: Duration.ZERO
                }
                BlockStatus.RUNNING -> actual += Progress.of(block, now)?.elapsed ?: Duration.ZERO
                BlockStatus.PENDING -> Unit
            }
        }

        return DaySummary(
            plannedTotal = day.blocks.fold(Duration.ZERO) { sum, b -> sum + b.planned },
            actualTotal = actual,
            done = done,
            skipped = skipped,
            total = day.blocks.size,
        )
    }
}
