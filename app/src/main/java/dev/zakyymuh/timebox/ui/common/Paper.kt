package dev.zakyymuh.timebox.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.zakyymuh.timebox.ui.theme.MonoLabel

/** A round, outlined button carrying a single glyph — the design's ‹ › style. */
@Composable
fun GlyphButton(glyph: String, onClick: () -> Unit, modifier: Modifier = Modifier, description: String? = null) {
    Box(
        modifier = modifier
            .size(36.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = glyph, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Back on the left, an uppercase mono title, and [actions] on the right. */
@Composable
fun PaperTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlyphButton("‹", onClick = onBack, description = "Back")
        Spacer(Modifier.width(12.dp))
        Text(
            text = title.uppercase(),
            style = MonoLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

/** A raised paper card. Clickable when [onClick] is given. */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(18.dp),
        content = content,
    )
}

/** A small uppercase section label. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MonoLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}
