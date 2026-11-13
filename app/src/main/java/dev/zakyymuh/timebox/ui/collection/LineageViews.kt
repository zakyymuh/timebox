package dev.zakyymuh.timebox.ui.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zakyymuh.timebox.core.collection.Dex
import dev.zakyymuh.timebox.core.collection.DexEntry
import dev.zakyymuh.timebox.core.collection.DexOrigin
import dev.zakyymuh.timebox.core.collection.Lineage
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small

/**
 * "From Kikko" or "From Kikko × Hakuun", under a plant's photo. A parent still in the
 * collection opens its page; one that left shows its kept name, greyed.
 */
@Composable
fun OriginCard(origin: DexOrigin, onOpenPlant: (Long) -> Unit, modifier: Modifier = Modifier) {
    SoftCard(color = Fresh.Pastel, corner = 20.dp, modifier = modifier) {
        Text(if (origin.fatherName == null) "Repotted from" else "Grown from a cross of", style = Label, color = Fresh.GreenDeep.copy(alpha = 0.75f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ParentLink(origin.motherName, origin.motherId, onOpenPlant)
            origin.fatherName?.let { father ->
                Text("  ×  ", style = MaterialTheme.typography.titleMedium, color = Fresh.GreenDeep)
                ParentLink(father, origin.fatherId, onOpenPlant)
            }
        }
        if (origin.motherId == null || (origin.fatherName != null && origin.fatherId == null)) {
            Text("A parent is no longer in your collection; its name is kept here.", style = Small, color = Fresh.InkSoft)
        }
    }
}

@Composable
private fun ParentLink(name: String, id: Long?, onOpenPlant: (Long) -> Unit) {
    Text(
        text = if (id != null) "$name →" else name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (id != null) Fresh.GreenDeep else Fresh.InkSoft,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (id != null) Modifier.clickable { onOpenPlant(id) } else Modifier)
            .padding(vertical = 4.dp),
    )
}

/** A titled list of plants, each opening its page. Nothing shows when [plants] is empty. */
@Composable
fun PlantList(title: String, plants: List<DexEntry>, onOpenPlant: (Long) -> Unit, modifier: Modifier = Modifier) {
    if (plants.isEmpty()) return
    Column(modifier) {
        Text("$title · ${plants.size}", style = Label, color = Fresh.InkSoft, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            plants.forEach { p ->
                SoftCard(corner = 20.dp, onClick = { onOpenPlant(p.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DexPhoto(p.photo, Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)), side = 160, emojiSize = 20.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium, color = Fresh.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(Dex.label(p.number), style = Label, color = Fresh.InkSoft)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Asks how many plants to add, 1 to [Lineage.MAX_COUNT], starting at [initial]. [extra]
 * sits under the count, e.g. a choice to remove the old pot.
 */
@Composable
fun CountDialog(
    title: String,
    text: String,
    confirm: (Int) -> String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    initial: Int = 5,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    var count by rememberSaveable { mutableIntStateOf(Lineage.count(initial)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fresh.Page,
        title = { Text(title) },
        text = {
            Column {
                Text(text, style = MaterialTheme.typography.bodyMedium, color = Fresh.InkSoft)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                ) {
                    StepButton("−", enabled = count > 1) { count = Lineage.count(count - 1) }
                    Text(
                        "$count",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Fresh.Ink,
                        modifier = Modifier.padding(horizontal = 28.dp),
                    )
                    StepButton("+", enabled = count < Lineage.MAX_COUNT) { count = Lineage.count(count + 1) }
                }
                extra()
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(count) }) { Text(confirm(count)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.headlineSmall,
        color = if (enabled) Fresh.Ink else Fresh.InkSoft.copy(alpha = 0.4f),
        modifier = Modifier
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 6.dp),
    )
}
