package dev.zakyymuh.timebox.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The wall, drawn as an overlay window straight from the service.
 *
 * This is the MIUI-proof path. Xiaomi silently drops background activity starts unless
 * the user has granted a separate "display pop-up windows while running in background"
 * permission, but an overlay window only needs SYSTEM_ALERT_WINDOW and is not subject
 * to that restriction at all.
 *
 * The spike can show the wall either way so we can find out which one this phone
 * actually honours. See [AppWatcherService.wallMode].
 */
class GateOverlay(private val service: AccessibilityService) {

    private var shown: View? = null

    val isShowing: Boolean get() = shown != null

    fun show(packageName: String, detectedAt: Long) {
        if (shown != null) return

        val wm = service.getSystemService(WindowManager::class.java) ?: return
        val view = buildView(packageName, detectedAt)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            // No NOT_TOUCHABLE and no NOT_FOCUSABLE: the button has to be tappable.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE,
        ).apply { gravity = Gravity.CENTER }

        try {
            wm.addView(view, params)
            shown = view
        } catch (e: Exception) {
            // Denied overlay permission, or an OEM refusing the window type.
            android.util.Log.w("Timebox", "overlay refused: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    fun hide() {
        val view = shown ?: return
        shown = null
        runCatching { service.getSystemService(WindowManager::class.java)?.removeView(view) }
    }

    private fun buildView(packageName: String, detectedAt: Long): View {
        val density = service.resources.displayMetrics.density
        val pad = (24 * density).toInt()
        val latency = if (detectedAt > 0L) SystemClock.elapsedRealtime() - detectedAt else -1L

        val headline = TextView(service).apply {
            text = "This isn't your plan."
            textSize = 32f
            setTextColor(Color.WHITE)
        }

        val detail = TextView(service).apply {
            text = buildString {
                append(packageName)
                append("\n\nwall mode: OVERLAY")
                if (latency >= 0) append("\nwall up in ${latency}ms")
            }
            textSize = 15f
            setTextColor(Color.parseColor("#9AA0A6"))
            setPadding(0, pad, 0, pad * 2)
        }

        val back = Button(service).apply {
            text = "Back to work"
            setOnClickListener {
                hide()
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }
        }

        return LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#101014"))
            setPadding(pad, pad, pad, pad)
            addView(headline)
            addView(detail)
            addView(back)
        }
    }
}
