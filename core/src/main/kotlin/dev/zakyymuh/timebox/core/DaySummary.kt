package dev.zakyymuh.timebox.core

/** How the day is going, for the widget's one-line summary and the list header. */
data class DaySummary(val done: Int, val total: Int) {
    val allDone: Boolean get() = total > 0 && done == total
    val isEmpty: Boolean get() = total == 0
}

object DaySummaries {
    fun summarise(day: DayList?): DaySummary {
        val items = day?.items ?: emptyList()
        return DaySummary(done = items.count { it.isDone }, total = items.size)
    }
}
