package dev.zakyymuh.timebox.ui.common

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.zakyymuh.timebox.ui.theme.Appearance
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small

/** The ⚙ at the top right of each app's home. */
@Composable
fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    RoundButton("⚙", onClick = onClick, modifier = modifier, size = 44.dp, description = "Settings")
}

/**
 * Settings, over the whole app: Account and Log out (both coming once there are accounts)
 * and light or dark. Picking a look repaints the app at once. × or Back closes it.
 *
 * Keep the open/closed flag in rememberSaveable: a new look recreates the activity, and
 * the page should still be open after it.
 */
@Composable
fun SettingsScreen(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val current = remember { Appearance.load(context) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Fresh.Page)
            // Swallows taps, so nothing underneath takes one.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .systemBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", style = MaterialTheme.typography.headlineLarge, color = Fresh.Ink, modifier = Modifier.weight(1f))
                RoundButton(glyph = "×", onClick = onClose, size = 40.dp, description = "Close")
            }

            Section("Account")
            SoonRow(icon = "👤", title = "Account", line = "Sign in and keep your things safe")

            Section("Appearance")
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Fresh.Card)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Appearance.entries.forEach { mode ->
                    val on = mode == current
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (on) Fresh.Page else Fresh.Card)
                            .clickable { (context as? Activity)?.let { Appearance.choose(it, mode) } }
                            .semantics {
                                role = Role.RadioButton
                                selected = on
                            }
                            .padding(vertical = 12.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                when (mode) {
                                    Appearance.System -> "📱"
                                    Appearance.Light -> "☀️"
                                    Appearance.Dark -> "🌙"
                                },
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                mode.label,
                                style = Label,
                                color = if (on) Fresh.Ink else Fresh.InkSoft,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.padding(top = 28.dp))
            SoonRow(icon = "↪", title = "Log out", line = "Once there are accounts")
            Spacer(Modifier.padding(bottom = 24.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = Label,
        color = Fresh.InkSoft,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 4.dp),
    )
}

/** A row that will do something later: greyed, with a "Soon" tag, and not tappable. */
@Composable
private fun SoonRow(icon: String, title: String, line: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Fresh.Card)
            .semantics(mergeDescendants = true) { contentDescription = "$title, coming soon" }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Fresh.Page).alpha(0.6f),
        ) {
            Text(icon, style = MaterialTheme.typography.titleMedium, color = Fresh.Ink)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).alpha(0.6f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Fresh.Ink)
            Text(line, style = Label, color = Fresh.InkSoft, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            "Soon",
            style = Small,
            color = Fresh.GreenDeep,
            modifier = Modifier
                .clip(CircleShape)
                .background(Fresh.Pastel)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
