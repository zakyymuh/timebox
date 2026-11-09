package dev.zakyymuh.timebox.core.motor

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The two warnings each date gets. [tag] is stored, so never change one. */
enum class ReminderStage(val tag: String, val days: Long) {
    MONTH("30d", 30),
    WEEK("7d", 7),
}

/**
 * A reminder that has gone out. Keyed by the date too, so renewing (which moves the
 * date) starts a fresh pair of reminders for the new one.
 */
data class SentReminder(val expiryId: Long, val date: LocalDate, val tag: String)

/** A notification to post now. */
data class Reminder(val expiryId: Long, val date: LocalDate, val stage: ReminderStage, val title: String, val text: String) {
    val sent: SentReminder get() = SentReminder(expiryId, date, stage.tag)
}

/**
 * Notify 30 days before and 7 days before. Checked once a day, so the check works on
 * ranges rather than exact days: a phone that was off on day 30 still warns on day 29.
 * At most one per date per day, and never the same stage twice.
 */
object ExpiryReminders {
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    /** [titles] names each expiry the way the app does: "SIM C", "STNK tax Avanza". */
    fun due(expiries: List<Expiry>, titles: Map<Long, String>, today: LocalDate, sent: Set<SentReminder>): List<Reminder> =
        expiries.mapNotNull { e ->
            val days = Expiries.daysLeft(e.date, today)
            val stage = when {
                days < 0 -> null
                days <= ReminderStage.WEEK.days -> ReminderStage.WEEK
                days <= ReminderStage.MONTH.days -> ReminderStage.MONTH
                else -> null
            } ?: return@mapNotNull null
            // Inside the last week only the week warning is due, never a late month one.
            if (SentReminder(e.id, e.date, stage.tag) in sent) return@mapNotNull null
            val name = titles[e.id] ?: e.name
            Reminder(
                expiryId = e.id,
                date = e.date,
                stage = stage,
                title = "$name expires ${whenText(days)}, ${e.date.format(dayMonth)}",
                text = "Renew it before ${e.date.format(dayMonthYear)}.",
            )
        }

    private fun whenText(days: Long): String = when (days) {
        0L -> "today"
        1L -> "tomorrow"
        7L -> "in 1 week"
        30L -> "in 1 month"
        else -> "in $days days"
    }
}
