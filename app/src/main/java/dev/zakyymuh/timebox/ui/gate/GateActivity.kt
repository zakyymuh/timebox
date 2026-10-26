package dev.zakyymuh.timebox.ui.gate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The wall. Covers the app the user just opened and asks them to go back to work.
 *
 * SPIKE: no intention, no reason field, no countdown, no pass — Task 6 builds those in
 * Compose. What this version proves is the part that cannot be proved any other way:
 * that the window actually appears over the blocked app on this phone, and how long it
 * takes to get there.
 */
class GateActivity : Activity() {

    private lateinit var detail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildView())
        render(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // singleInstance reuses this activity, so a second detection arrives here.
        intent?.let { render(it) }
    }

    private fun render(from: Intent) {
        val pkg = from.getStringExtra(EXTRA_PACKAGE) ?: "unknown"
        val detectedAt = from.getLongExtra(EXTRA_DETECTED_AT, 0L)
        val latency = if (detectedAt > 0L) SystemClock.elapsedRealtime() - detectedAt else -1L
        detail.text = buildString {
            append(pkg)
            append("\n\nwall mode: ACTIVITY")
            if (latency >= 0) {
                append("\nwall up in ${latency}ms")
            }
        }
    }

    private fun buildView(): ViewGroup {
        val pad = (24 * resources.displayMetrics.density).toInt()

        val headline = TextView(this).apply {
            text = "This isn't your plan."
            textSize = 34f
            setTextColor(Color.WHITE)
        }

        detail = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.parseColor("#9AA0A6"))
            setPadding(0, pad, 0, pad * 2)
        }

        val back = Button(this).apply {
            text = "Back to work"
            setOnClickListener { goHome() }
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad, pad, pad, pad)
            addView(headline)
            addView(detail)
            addView(back)
        }
    }

    /**
     * Finishing alone would uncover the blocked app, so leave by going home instead.
     * Back does the same thing for the same reason.
     */
    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finishAndRemoveTask()
    }

    @Deprecated("Fine for the spike; Task 6 uses onBackPressedDispatcher.")
    override fun onBackPressed() {
        goHome()
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
        private const val EXTRA_DETECTED_AT = "detected_at"

        fun intent(context: Context, packageName: String, detectedAt: Long): Intent =
            Intent(context, GateActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_DETECTED_AT, detectedAt)
    }
}
