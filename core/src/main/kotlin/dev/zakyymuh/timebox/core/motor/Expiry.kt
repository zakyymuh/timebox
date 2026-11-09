package dev.zakyymuh.timebox.core.motor

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Something that runs out on a date: your SIM C, or a vehicle's STNK tax. [vehicleId] is
 * null when it is yours rather than a vehicle's. [renewMonths] is how far one renewal
 * moves the date: 12 for the yearly STNK tax, 60 for a SIM.
 */
data class Expiry(
    val id: Long,
    val vehicleId: Long?,
    val name: String,
    val date: LocalDate,
    val renewMonths: Int,
)

/** A ready-made kind of expiry, to fill in the name and renewal in one tap. */
data class ExpiryKind(val name: String, val renewMonths: Int)

/** What the expiry edit screen holds. */
data class ExpiryForm(
    val vehicleId: Long? = null,
    val name: String = "",
    val date: LocalDate,
    val renewMonths: Int = 12,
) {
    fun isValid(): Boolean = name.isNotBlank() && name.trim().length <= MAX_NAME && renewMonths in 1..MAX_RENEW_MONTHS

    companion object {
        const val MAX_NAME = 40
        const val MAX_RENEW_MONTHS = 240
    }
}

object Expiries {
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    /** Yours: driving licences and travel papers. */
    val personal = listOf(
        ExpiryKind("SIM A", 60),
        ExpiryKind("SIM C", 60),
        ExpiryKind("Passport", 120),
        ExpiryKind("International licence", 36),
    )

    /** Each vehicle's papers. */
    val vehicle = listOf(
        ExpiryKind("STNK tax", 12),
        ExpiryKind("STNK 5-year", 60),
        ExpiryKind("Insurance", 12),
        ExpiryKind("KIR", 6),
    )

    /** Negative once it has passed. */
    fun daysLeft(date: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(today, date)

    /** It still counts on its last day, so overdue starts the day after. */
    fun isOverdue(date: LocalDate, today: LocalDate): Boolean = date.isBefore(today)

    /** The 30 days before a date, when it shows as needing you. */
    fun isSoon(date: LocalDate, today: LocalDate): Boolean = daysLeft(date, today) in 0..SOON_DAYS

    /** Soonest first; overdue ones are soonest of all. */
    fun soonestFirst(expiries: List<Expiry>): List<Expiry> =
        expiries.sortedWith(compareBy<Expiry> { it.date }.thenBy { it.name.lowercase() }.thenBy { it.id })

    /** One renewal: the date moves on by the renewal period, keeping its day of the year. */
    fun renewed(expiry: Expiry): LocalDate = expiry.date.plusMonths(expiry.renewMonths.toLong())

    /** "expires 12 Mar 2027 · in 4 months", or "expired 12 Mar 2026 · 3 days ago". */
    fun line(date: LocalDate, today: LocalDate): String {
        val on = date.format(dayMonthYear)
        return if (isOverdue(date, today)) "expired $on · ${ago(date, today)}" else "expires $on · ${until(date, today)}"
    }

    /** "today", "tomorrow", "in 9 days", "in 4 months", "in 2 years". */
    fun until(date: LocalDate, today: LocalDate): String {
        val days = daysLeft(date, today)
        return when {
            days <= 0L -> "today"
            days == 1L -> "tomorrow"
            days <= 45 -> "in $days days"
            else -> "in " + span(Period.between(today, date))
        }
    }

    private fun ago(date: LocalDate, today: LocalDate): String {
        val past = ChronoUnit.DAYS.between(date, today)
        return when {
            past == 1L -> "yesterday"
            past <= 45 -> "$past days ago"
            else -> span(Period.between(date, today)) + " ago"
        }
    }

    /** Whole months, or whole years from a year on. */
    private fun span(period: Period): String {
        val months = period.toTotalMonths().coerceAtLeast(1)
        return if (months < 12) {
            if (months == 1L) "1 month" else "$months months"
        } else {
            val years = months / 12
            if (years == 1L) "1 year" else "$years years"
        }
    }

    /** "1 year", "5 years", "6 months": how a renewal period reads. */
    fun period(months: Int): String = when {
        months % 12 == 0 -> if (months == 12) "1 year" else "${months / 12} years"
        months == 1 -> "1 month"
        else -> "$months months"
    }

    const val SOON_DAYS = 30L
}
