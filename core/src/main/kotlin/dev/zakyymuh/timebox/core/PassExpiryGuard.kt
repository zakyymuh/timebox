package dev.zakyymuh.timebox.core

import java.time.Clock

/** Somewhere to put a delayed action, keyed so it can be replaced or dropped. */
interface DelayScheduler {
    fun postDelayed(key: String, delayMillis: Long, action: () -> Unit)
    fun cancel(key: String)
}

/**
 * Arms a re-check for the moment a pass runs out.
 *
 * Without this, a pass that expires while the user is still inside the app is never
 * noticed: no window change fires, so nothing re-evaluates, and five minutes quietly
 * becomes an hour.
 */
class PassExpiryGuard(
    private val scheduler: DelayScheduler,
    private val clock: Clock,
) {

    fun arm(pass: Pass, onExpiry: (String) -> Unit) {
        scheduler.cancel(pass.packageName)
        val delay = (pass.expiresAt.toEpochMilli() - clock.millis()).coerceAtLeast(0L)
        scheduler.postDelayed(pass.packageName, delay) { onExpiry(pass.packageName) }
    }
}
