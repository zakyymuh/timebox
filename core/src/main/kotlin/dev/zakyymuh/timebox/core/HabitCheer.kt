package dev.zakyymuh.timebox.core

/** One encouraging line under the dashboard's "2 /5 done". */
object HabitCheer {
    fun of(summary: DaySummary): String = when {
        summary.isEmpty -> "Plan your day"
        summary.allDone -> "All done, well done 🎉"
        summary.done == 0 -> "Let's get started"
        summary.done * 2 >= summary.total -> "Over halfway!"
        else -> "Good start, keep going"
    }
}
