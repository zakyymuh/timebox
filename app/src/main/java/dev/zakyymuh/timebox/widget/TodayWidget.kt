package dev.zakyymuh.timebox.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import dev.zakyymuh.timebox.ServiceLocator
import dev.zakyymuh.timebox.ui.MainActivity
import dev.zakyymuh.timebox.core.TodoItem
import dev.zakyymuh.timebox.core.WidgetView
import dev.zakyymuh.timebox.core.WidgetViews

/**
 * Today's list on the home screen.
 *
 * Reads the database directly in [provideGlance], so it works from a cold-started
 * process with no Activity alive. What it shows is decided by [WidgetViews] in :core,
 * which is tested without Android.
 */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val day = ServiceLocator.repository(context.applicationContext).todayNow()
        val view = WidgetViews.of(day, maxVisible = MAX_VISIBLE)

        provideContent {
            GlanceTheme {
                Body(view)
            }
        }
    }

    @Composable
    private fun Body(view: WidgetView) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(14.dp)
                // Tapping anywhere opens the app; the widget is a glance, not an editor.
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            when (view) {
                is WidgetView.NeedsList -> NeedsList()
                is WidgetView.HasList -> HasList(view)
            }
        }
    }

    @Composable
    private fun NeedsList() {
        Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Nothing on today's list",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp),
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = "Tap to write it",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            )
        }
    }

    @Composable
    private fun HasList(view: WidgetView.HasList) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "Today",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp),
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = "${view.summary.done}/${view.summary.total}",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            )
        }

        Spacer(GlanceModifier.height(8.dp))

        view.visible.forEach { item ->
            ItemLine(item)
        }

        if (view.hidden > 0) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = "+${view.hidden} more",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
        }
    }

    @Composable
    private fun ItemLine(item: TodoItem) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A glyph rather than a real checkbox: tapping a widget row to tick things
            // off invites misses, and a wrong tap on a glance is worse than a trip
            // into the app.
            Text(
                text = if (item.isDone) "✓" else "·",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = item.text,
                maxLines = 1,
                style = TextStyle(
                    color = if (item.isDone) {
                        GlanceTheme.colors.onSurfaceVariant
                    } else {
                        GlanceTheme.colors.onSurface
                    },
                    fontSize = 14.sp,
                    textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None,
                ),
            )
        }
    }

    private companion object {
        const val MAX_VISIBLE = 5
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
