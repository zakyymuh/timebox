package dev.zakyymuh.timebox.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import dev.zakyymuh.timebox.ui.theme.Appearance
import dev.zakyymuh.timebox.ui.theme.Fresh
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import dev.zakyymuh.timebox.nudge.EveningReminder
import dev.zakyymuh.timebox.ui.theme.DailyTheme

/** How long the launch rosette takes to turn; matches splash_rosette.xml. */
private const val SPLASH_MS = 2_000L

class MainActivity : ComponentActivity() {

    private val askNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* either way, carry on */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light or dark, as picked in Settings; the system bars match the page.
        Appearance.start(this, Fresh.Rose, Fresh.RoseDark)

        // Re-arming an already-armed alarm is harmless, and this covers a first launch
        // and any firing that was missed while the app was not running.
        EveningReminder(applicationContext).apply {
            ensureChannel()
            schedule()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            DailyTheme {
                AppNav(start = Destinations.from(intent))
            }
        }

        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) holdSplash()
    }

    /**
     * The system drops the splash on the first frame, which would cut the slow rosette
     * short. So the first frame waits until the turn is done, counted from when the app
     * process started. When the app was already running, that time has long passed and
     * nothing waits.
     */
    private fun holdSplash() {
        val until = Process.getStartUptimeMillis() + SPLASH_MS
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val left = until - SystemClock.uptimeMillis()
                if (left > 0) {
                    content.postDelayed({ content.invalidate() }, left)
                    return false
                }
                content.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
    }
}
