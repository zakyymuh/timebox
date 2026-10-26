package dev.zakyymuh.timebox.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import dev.zakyymuh.timebox.core.ForegroundFilter
import dev.zakyymuh.timebox.ui.gate.GateActivity

/**
 * Watches which app comes to the foreground and raises the gate over the off-plan ones.
 *
 * SPIKE: the blocked set is hardcoded and there is no day plan, pass or database.
 * Task 4 replaces [SPIKE_BLOCKED] with the user's picked apps and Task 5 routes the
 * decision through GateDecider. The event handling, the filter wiring and both wall
 * paths are the real thing.
 *
 * Two wall paths exist because MIUI silently drops background activity starts without a
 * Xiaomi-specific permission, while overlay windows need only SYSTEM_ALERT_WINDOW.
 * The spike can use either so we can learn which this phone honours.
 */
class AppWatcherService : AccessibilityService() {

    private val filter = ForegroundFilter(OWN_PACKAGE)
    private val overlay by lazy { GateOverlay(this) }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        filter.reset()
        Log.i(TAG, "connected; mode=${wallMode(this)}; watching ${SPIKE_BLOCKED.size} packages")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val detectedAt = SystemClock.elapsedRealtime()
        val pkg = event.packageName?.toString()

        // The filter drops our own windows and collapses the several events one app
        // launch fires. Without it the gate would gate itself, over and over.
        if (!filter.onWindow(pkg)) return

        if (pkg == null || pkg !in SPIKE_BLOCKED) {
            // Left the blocked app, so take the overlay down with it.
            if (overlay.isShowing) overlay.hide()
            return
        }

        when (wallMode(this)) {
            WallMode.OVERLAY -> {
                Log.i(TAG, "blocking $pkg via overlay")
                overlay.show(pkg, detectedAt)
            }
            WallMode.ACTIVITY -> {
                Log.i(TAG, "blocking $pkg via activity")
                startActivity(GateActivity.intent(this, pkg, detectedAt))
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        overlay.hide()
        instance = null
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        overlay.hide()
        instance = null
        return super.onUnbind(intent)
    }

    enum class WallMode { OVERLAY, ACTIVITY }

    companion object {
        private const val TAG = "Timebox"
        private const val OWN_PACKAGE = "dev.zakyymuh.timebox"
        private const val PREFS = "timebox_spike"
        private const val KEY_MODE = "wall_mode"

        @Volatile
        private var instance: AppWatcherService? = null

        /** True only while the service process is alive and connected. */
        val isRunning: Boolean get() = instance != null

        fun wallMode(context: Context): WallMode {
            val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_MODE, WallMode.OVERLAY.name)
            return runCatching { WallMode.valueOf(name!!) }.getOrDefault(WallMode.OVERLAY)
        }

        fun setWallMode(context: Context, mode: WallMode) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_MODE, mode.name).apply()
        }

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
