package dev.zakyymuh.timebox.service

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import dev.zakyymuh.timebox.core.ForegroundFilter
import dev.zakyymuh.timebox.ui.gate.GateActivity

/**
 * Watches which app comes to the foreground and raises the gate over the off-plan ones.
 *
 * SPIKE: the blocked set is hardcoded below and there is no day plan, no pass and no
 * database. Task 4 replaces [SPIKE_BLOCKED] with the user's picked apps, and Task 5
 * routes the decision through GateDecider. The event handling, the filter wiring and
 * the activity launch are the real thing.
 */
class AppWatcherService : AccessibilityService() {

    private val filter = ForegroundFilter(OWN_PACKAGE)

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        filter.reset()
        Log.i(TAG, "connected; watching ${SPIKE_BLOCKED.size} packages")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val detectedAt = SystemClock.elapsedRealtime()
        val pkg = event.packageName?.toString()

        // The filter drops our own windows and collapses the several events one app
        // launch fires. Without it the gate would gate itself, over and over.
        if (!filter.onWindow(pkg)) return
        if (pkg == null || pkg !in SPIKE_BLOCKED) return

        Log.i(TAG, "blocking $pkg")
        startActivity(GateActivity.intent(this, pkg, detectedAt))
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        isRunning = false
        return super.onUnbind(intent)
    }

    /** Sends the user home. Only the service can do this, so the gate asks it to. */
    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    companion object {
        private const val TAG = "Timebox"
        private const val OWN_PACKAGE = "dev.zakyymuh.timebox"

        @Volatile
        var isRunning: Boolean = false
            private set

        /** SPIKE only. Task 4 replaces this with what the user picks. */
        val SPIKE_BLOCKED = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",   // TikTok
            "com.ss.android.ugc.trill",   // TikTok, other regions
            "com.twitter.android",
            "com.x.android",
            "com.google.android.youtube",
            "com.facebook.katana",
            "com.whatsapp",
            "com.reddit.frontpage",
            "com.shopee.id",
            "com.tokopedia.tkpd",
        )
    }
}
