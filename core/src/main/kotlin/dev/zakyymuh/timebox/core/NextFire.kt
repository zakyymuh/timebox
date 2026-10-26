package dev.zakyymuh.timebox.core

import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * When a daily alarm should next go off.
 *
 * Works in local wall-clock time so a 06:30 reminder stays at 06:30 across a daylight
 * saving change rather than drifting by an hour.
 */
object NextFire {

    fun compute(at: LocalTime, now: ZonedDateTime): ZonedDateTime {
        val todays = ZonedDateTime.of(now.toLocalDate(), at, now.zone)
        return if (todays.isAfter(now)) todays else ZonedDateTime.of(now.toLocalDate().plusDays(1), at, now.zone)
    }
}
