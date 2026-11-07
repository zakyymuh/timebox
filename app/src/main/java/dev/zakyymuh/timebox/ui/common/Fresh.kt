package dev.zakyymuh.timebox.ui.common

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Small

/** A round button carrying one glyph — the reference's ▶ style. */
@Composable
fun RoundButton(
    glyph: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Fresh.Card,
    content: Color = Fresh.Ink,
    size: Dp = 48.dp,
    description: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick)
            .let { m -> if (description != null) m.semantics { contentDescription = description } else m },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = glyph, style = MaterialTheme.typography.titleMedium, color = content)
    }
}

/** A big soft card: no outline, just colour and rounded corners. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = Fresh.Card,
    corner: Dp = 28.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .background(color)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(20.dp),
        content = content,
    )
}

/** A small fully-round tag, like the reference's "5 min". */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = Fresh.Page, textColor: Color = Fresh.Ink) {
    Text(
        text = text,
        style = Small,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(CircleShape)
            .background(color)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** A round back button, a bold title, and [actions] on the right. */
@Composable
fun TopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundButton("‹", onClick = onBack, description = "Back")
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Fresh.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

/** A section heading in sentence case, like the reference's "Special for you". */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = Fresh.Ink,
        modifier = modifier.padding(top = 24.dp, bottom = 12.dp),
    )
}
