package dev.zakyymuh.timebox.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import dev.zakyymuh.timebox.service.AppWatcherService
import dev.zakyymuh.timebox.service.ServiceStatus

/**
 * SPIKE: permission status, the settings deep links, and a switch between the two wall
 * modes so we can learn which one this phone honours. Task 3 replaces this with the
 * Compose home screen and Task 8 with the real setup screen.
 */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var watching: TextView
    private lateinit var modeButton: Button

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
        val mode = AppWatcherService.wallMode(this)

        watching.text = if (a11y && overlay) "WATCHING" else "NOT WATCHING"
        watching.setTextColor(
            if (a11y && overlay) Color.parseColor("#1B873F") else Color.parseColor("#C5221F"),
        )

        modeButton.text = "Wall mode: $mode  (tap to switch)"

        status.text = buildString {
            appendLine("Android:                ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device:                 ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine()
            appendLine("Accessibility enabled:  ${yn(a11y)}")
            appendLine("Display over apps:      ${yn(overlay)}")
            appendLine("Service alive:          ${yn(AppWatcherService.isRunning)}")
            appendLine()
            appendLine("Open any of these to test the wall:")
            AppWatcherService.SPIKE_BLOCKED.sorted().forEach { appendLine("  · $it") }
        }
    }

    private fun yn(b: Boolean) = if (b) "yes" else "NO"

    private fun buildView(): ViewGroup {
        val pad = (20 * resources.displayMetrics.density).toInt()

        fun heading(t: String) = TextView(this).apply {
            text = t
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, pad, 0, pad / 3)
        }

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
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }

        modeButton = Button(this).apply {
            setOnClickListener {
                val next = if (AppWatcherService.wallMode(this@MainActivity) == AppWatcherService.WallMode.OVERLAY) {
                    AppWatcherService.WallMode.ACTIVITY
                } else {
                    AppWatcherService.WallMode.OVERLAY
                }
                AppWatcherService.setWallMode(this@MainActivity, next)
                refresh()
            }
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(title)
            addView(watching)
            addView(status)

            addView(heading("1 · Required permissions"))
            addView(
                button("Accessibility settings") {
                    open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
            )
            addView(
                button("Display over other apps") {
                    open(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName"),
                        ),
                    )
                },
            )

            addView(heading("2 · Which wall to use"))
            addView(modeButton)
            addView(
                note(
                    "OVERLAY draws the wall from the service and needs only " +
                        "\"display over other apps\". ACTIVITY launches a full screen, which " +
                        "MIUI blocks unless you also allow background pop-up windows. Try " +
                        "both and tell me which one actually appears.",
                ),
            )

            if (isXiaomi()) {
                addView(heading("3 · MIUI, and this is the part that matters"))
                addView(
                    note(
                        "MIUI needs three things beyond the two above, or Timebox will " +
                            "either never fire or quietly die after an hour:\n\n" +
                            "· Other permissions → \"Display pop-up windows while running in " +
                            "background\" → allow. Without this, ACTIVITY mode silently does nothing.\n\n" +
                            "· Autostart → on. Without it the service does not come back after a reboot.\n\n" +
                            "· Battery saver → No restrictions. Otherwise MIUI kills the service when idle.",
                    ),
                )
                addView(button("MIUI: other permissions") { openMiuiPermissions() })
                addView(button("MIUI: autostart") { openMiuiAutostart() })
                addView(
                    button("Battery settings") {
                        open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                    },
                )
            }

            addView(
                note(
                    "If the Accessibility toggle is greyed out, this APK was sideloaded: " +
                        "Settings → Apps → Timebox → ⋮ → Allow restricted settings, then come back.",
                ),
            )
        }

        return ScrollView(this).apply { addView(column) }
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }

    private fun note(t: String) = TextView(this).apply {
        text = t
        textSize = 12f
        setTextColor(Color.parseColor("#5F6368"))
        setPadding(0, 8, 0, 8)
    }

    private fun isXiaomi(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        return "xiaomi" in m || "redmi" in m || "poco" in m
    }

    /** MIUI's per-app permission editor. OEM-private, so failure is expected and handled. */
    private fun openMiuiPermissions() {
        val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
            setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity",
            )
            putExtra("extra_pkgname", packageName)
        }
        openOrFallback(intent, "Open Security app → Permissions → Timebox")
    }

    private fun openMiuiAutostart() {
        val intent = Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
        )
        openOrFallback(intent, "Open Security app → Autostart")
    }

    private fun openOrFallback(intent: Intent, hint: String) {
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Not available on this build. $hint", Toast.LENGTH_LONG).show()
            open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Could not open that settings screen.", Toast.LENGTH_LONG).show()
        }
    }
}
