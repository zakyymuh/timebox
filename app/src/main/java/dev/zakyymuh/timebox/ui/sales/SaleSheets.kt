package dev.zakyymuh.timebox.ui.sales

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.zakyymuh.timebox.core.collection.DexForm
import dev.zakyymuh.timebox.core.collection.SaleForm
import dev.zakyymuh.timebox.core.collection.SaleKind
import dev.zakyymuh.timebox.core.collection.Sales
import dev.zakyymuh.timebox.ui.common.DateField
import dev.zakyymuh.timebox.ui.common.softFieldColors
import dev.zakyymuh.timebox.ui.plants.Choice
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small

/** Rupiah, digits only, at most nine of them. */
@Composable
internal fun RupiahField(value: Long?, hint: String, modifier: Modifier = Modifier, onChange: (Long?) -> Unit) {
    TextField(
        value = value?.toString() ?: "",
        onValueChange = { text -> onChange(text.filter(Char::isDigit).take(9).toLongOrNull()) },
        placeholder = { Text(hint, color = Fresh.InkSoft, style = Small) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(20.dp),
        colors = softFieldColors(),
        modifier = modifier,
    )
}

/** Put a plant up for sale (or take it off) with an asking price; or go record a sale. */
@Composable
fun SellSheet(
    name: String,
    forSale: Boolean,
    asking: Long?,
    onSave: (forSale: Boolean, asking: Long?) -> Unit,
    onRecord: () -> Unit,
    onDismiss: () -> Unit,
) {
    var on by remember { mutableStateOf(forSale) }
    var price by remember { mutableStateOf(asking) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fresh.Page,
        title = { Text("Sell $name") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("For sale", style = MaterialTheme.typography.titleMedium, color = Fresh.Ink)
                        Text("Shows a SALE ribbon in your collection", style = Small, color = Fresh.InkSoft)
                    }
                    Switch(
                        checked = on,
                        onCheckedChange = { on = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Fresh.Green),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text("Asking price, each (optional)", style = Label, color = Fresh.InkSoft, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                RupiahField(price, "Rp, e.g. 150000", Modifier.fillMaxWidth()) { price = it }
                Spacer(Modifier.height(14.dp))
                TextButton(onClick = onRecord) {
                    Text("💲  Record a sale →", style = MaterialTheme.typography.labelLarge, color = Fresh.GreenDeep)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(on, price) },
                enabled = (price ?: 0) <= DexForm.MAX_PRICE,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * What went out, how many, for how much, and when. The price follows asking × quantity
 * until it is typed over. With [onDelete] it is editing a sale already kept.
 */
@Composable
fun RecordSaleSheet(
    title: String,
    initial: SaleForm,
    asking: Long?,
    onSave: (SaleForm) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var form by remember { mutableStateOf(initial) }
    var typed by remember { mutableStateOf(initial.price != null) }
    val shown = Sales.normalise(form).let { f -> if (typed) f else f.copy(price = Sales.suggestedPrice(asking, f.quantity)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Fresh.Page,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    SaleKind.entries.forEach { kind ->
                        Choice(kind.label, selected = form.kind == kind) { form = form.copy(kind = kind) }
                    }
                }
                if (form.kind != SaleKind.WHOLE) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("How many", style = MaterialTheme.typography.titleMedium, color = Fresh.Ink, modifier = Modifier.weight(1f))
                        TextButton(onClick = { form = form.copy(quantity = (form.quantity - 1).coerceAtLeast(1)) }) { Text("−") }
                        Text("${form.quantity}", style = MaterialTheme.typography.titleMedium, color = Fresh.Ink)
                        TextButton(onClick = { form = form.copy(quantity = (form.quantity + 1).coerceAtMost(SaleForm.MAX_QUANTITY)) }) { Text("+") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Sold for (total)", style = Label, color = Fresh.InkSoft, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                RupiahField(shown.price, "Rp, e.g. 150000", Modifier.fillMaxWidth()) {
                    typed = true
                    form = form.copy(price = it)
                }
                Spacer(Modifier.height(12.dp))
                DateField("Sold on", form.soldOn) { form = form.copy(soldOn = it) }
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = form.note,
                    onValueChange = { form = form.copy(note = it.take(DexForm.MAX_NOTE)) },
                    placeholder = { Text("Note: who bought it, where…", color = Fresh.InkSoft, style = Small) },
                    shape = RoundedCornerShape(20.dp),
                    colors = softFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (onDelete != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDelete) { Text("Delete this sale", color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(shown) }, enabled = shown.isValid) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
