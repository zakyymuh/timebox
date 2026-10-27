package dev.zakyymuh.timebox.core

/** What the home-screen widget should show. Pure, so it is tested without Android. */
sealed interface WidgetView {

    /** Nothing written for today — where every morning starts. */
    data class NeedsList(val suggestionCount: Int) : WidgetView

    data class HasList(
        val note: String?,
        /** The items to show, already trimmed to what fits. */
        val visible: List<TodoItem>,
        /** How many more there are below the fold, or zero. */
        val hidden: Int,
        val summary: DaySummary,
    ) : WidgetView
}

object WidgetViews {

    /**
     * Unfinished things come first: a widget is a glance, and the useful glance is what
     * is left rather than what is already done. Done items fill any remaining room so
     * the day still reads as a record.
     */
    fun of(day: DayList?, maxVisible: Int): WidgetView {
        val items = day?.items ?: emptyList()
        if (items.isEmpty()) return WidgetView.NeedsList(suggestionCount = 0)

        val ordered = items.filterNot { it.isDone } + items.filter { it.isDone }
        return WidgetView.HasList(
            note = day?.note,
            visible = ordered.take(maxVisible),
            hidden = (ordered.size - maxVisible).coerceAtLeast(0),
            summary = DaySummaries.summarise(day),
        )
    }
}
