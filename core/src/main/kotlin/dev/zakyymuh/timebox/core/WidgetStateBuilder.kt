package dev.zakyymuh.timebox.core

/** What the home-screen widget should show. */
sealed interface WidgetState {

    /** No intention written for today: offer the past ones as one-tap buttons. */
    data class NeedsNote(val suggestions: List<String>) : WidgetState

    data class HasNote(
        val intention: String,
        val stopsToday: Int,
        /** False when the accessibility service is off, so the widget can say so. */
        val watching: Boolean,
    ) : WidgetState
}

class WidgetStateBuilder {

    /**
     * [plan] is today's intention or null. A plan from any earlier day must never reach
     * here as non-null, or the widget would show a stale intention after midnight.
     */
    fun build(
        plan: DayPlan?,
        recentIntentions: List<String>,
        stopsToday: Int,
        watching: Boolean,
        maxSuggestions: Int,
    ): WidgetState =
        if (plan == null) {
            WidgetState.NeedsNote(recentIntentions.take(maxSuggestions))
        } else {
            WidgetState.HasNote(plan.intention, stopsToday, watching)
        }
}
