package dev.zakyymuh.timebox.core

import java.time.Instant
import java.time.LocalDate

/** One day's intention — what today is for, written in the morning. */
data class DayPlan(val date: LocalDate, val intention: String, val createdAt: Instant)

/** An app the user has put off-plan. */
data class BlockedApp(val packageName: String, val label: String)

/**
 * Permission to use an off-plan app until [expiresAt].
 *
 * Expiry is wall-clock on purpose: a pass granted at 09:00 for five minutes is over
 * at 09:05 whether or not the phone rebooted in between. Nothing here may be derived
 * from uptime.
 */
data class Pass(
    val id: Long,
    val packageName: String,
    val grantedAt: Instant,
    val expiresAt: Instant,
    val reason: String,
)

enum class GateOutcome { RETURNED_TO_WORK, PASS_GRANTED }

data class GateEvent(
    val id: Long,
    val packageName: String,
    val at: Instant,
    val outcome: GateOutcome,
)
