package dev.zakyymuh.timebox.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import dev.zakyymuh.timebox.service.AppWatcherService
import dev.zakyymuh.timebox.service.ServiceStatus

/**
 * SPIKE: permission status and the two deep links, so the mechanism can be switched on
 * and tested. Task 3 replaces this with the Compose home screen and morning note, and
 * Task 8 with the real setup screen.
 */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var watching: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildView())
    }

    // The user grants these outside the app, so re-read every time they come back.
    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val a11y = ServiceStatus.isAccessibilityEnabled(this)
        val overlay = ServiceStatus.canDrawOverlays(this)
        val live = AppWatcherService.isRunning

        watching.text = if (a11y && overlay) "WATCHING" else "NOT WATCHING"
        watching.setTextColor(
            if (a11y && overlay) Color.parseColor("#1B873F") else Color.parseColor("#C5221F"),
        )

        status.text = buildString {
            appendLine("Accessibility enabled:  ${yn(a11y)}")
            appendLine("Display over apps:      ${yn(overlay)}")
            appendLine("Service process alive:  ${yn(live)}")
            appendLine()
            appendLine("Open any of these to test the wall:")
            AppWatcherService.SPIKE_BLOCKED.sorted().forEach { appendLine("  · $it") }
        }
    }

    private fun yn(b: Boolean) = if (b) "yes" else "NO"

    private fun buildView(): ViewGroup {
        val pad = (20 * resources.displayMetrics.density).toInt()

        val title = TextView(this).apply {
            text = "Timebox · mechanism spike"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
        }

        watching = TextView(this).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, pad / 2, 0, pad / 2)
        }

        status = TextView(this).apply {
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, pad)
        }

        val a11yButton = Button(this).apply {
            text = "Open Accessibility settings"
            setOnClickListener { open(Settings.ACTION_ACCESSIBILITY_SETTINGS) }
        }

        val overlayButton = Button(this).apply {
            text = "Open Display-over-apps settings"
            setOnClickListener {
                open(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            }
        }

        val hint = TextView(this).apply {
            text = "If the Accessibility toggle is greyed out, this APK was sideloaded: " +
                "Settings → Apps → Timebox → ⋮ → Allow restricted settings, then come back."
            textSize = 12f
            setTextColor(Color.parseColor("#5F6368"))
            setPadding(0, pad, 0, 0)
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(title)
            addView(watching)
            addView(status)
            addView(a11yButton)
            addView(overlayButton)
            addView(hint)
        }

        return ScrollView(this).apply { addView(column) }
    }

    private fun open(action: String, data: Uri? = null) {
        startActivity(Intent(action).apply { data?.let { setData(it) } })
    }
}
