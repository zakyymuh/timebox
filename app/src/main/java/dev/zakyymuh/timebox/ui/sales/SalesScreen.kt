package dev.zakyymuh.timebox.ui.sales

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.core.collection.Sale
import dev.zakyymuh.timebox.core.collection.SaleForm
import dev.zakyymuh.timebox.core.collection.Sales
import dev.zakyymuh.timebox.core.collection.SalesTotals
import dev.zakyymuh.timebox.core.motor.Money
import androidx.compose.ui.platform.LocalContext
import dev.zakyymuh.timebox.core.collection.DexEntry
import dev.zakyymuh.timebox.ui.collection.DexCard
import dev.zakyymuh.timebox.ui.collection.ShareCard
import dev.zakyymuh.timebox.ui.collection.shareSubtitle
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.plants.Choice
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val soldFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

/**
 * What went out and for how much: the totals, then two tabs. For sale is a grid of the
 * plants up for sale; Sold is every sale, newest first. Tap opens the plant, a long press
 * changes or deletes the sale.
 */
@Composable
fun SalesScreen(viewModel: SalesViewModel, onBack: () -> Unit, onOpenPlant: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<Sale?>(null) }

    editing?.let { sale ->
        RecordSaleSheet(
            title = "Change this sale",
            initial = SaleForm(sale.kind, sale.quantity, sale.price, sale.soldOn, sale.note),
            asking = null,
            onSave = { form ->
                editing = null
                viewModel.edit(sale.id, form)
            },
            onDismiss = { editing = null },
            onDelete = {
                editing = null
                viewModel.delete(sale.id)
            },
        )
    }

    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(Modifier.padding(insets).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                TopBar(title = "Sales", onBack = onBack)
            }
            val s = state ?: return@Column
            TotalsCard(s.totals, Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
                Choice("For sale · ${s.forSale.size}", selected = pager.currentPage == 0) { scope.launch { pager.animateScrollToPage(0) } }
                Choice("Sold · ${s.sold.size}", selected = pager.currentPage == 1) { scope.launch { pager.animateScrollToPage(1) } }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) { page ->
                Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    if (page == 0) {
                        if (s.forSale.isEmpty()) {
                            Hint("Nothing for sale. Open a plant in your collection and tap \$.")
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 32.dp),
                            ) {
                                items(s.forSale, key = { it.id }) { plant ->
                                    ForSaleCard(plant, onOpen = { onOpenPlant(plant.id) }) {
                                        scope.launch {
                                            ShareCard.sharePlantForSale(context, plant.photo, plant.name, plant.askingPrice, shareSubtitle(plant))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (s.sold.isEmpty()) {
                            Hint("Nothing sold yet.")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                                items(s.sold, key = { it.id }) { sale ->
                                    SaleRow(sale, onOpen = { sale.dexId?.let(onOpenPlant) }, onLongPress = { editing = sale })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A plant on the shelf: the rose frame, its asking price in a white pill on the photo (none
 * without a price), and a round share button inside the photo's top-right corner that
 * shares it as a story without opening the card.
 */
@Composable
private fun ForSaleCard(plant: DexEntry, onOpen: () -> Unit, onShare: () -> Unit) {
    DexCard(
        plant,
        modifier = Modifier.saleBorder(24.dp),
        overlay = {
            val price = plant.askingPrice
            if (price != null) PricePill(price, Modifier.align(Alignment.BottomStart).padding(8.dp))
            StoryShareButton(onClick = onShare, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
        },
        onClick = onOpen,
    )
}

@Composable
private fun TotalsCard(totals: SalesTotals, modifier: Modifier) {
    SoftCard(color = Fresh.Pastel, corner = 24.dp, modifier = modifier.fillMaxWidth()) {
        Text("All time", style = Label, color = Fresh.GreenDeep.copy(alpha = 0.75f))
        Text(
            "${totals.items} sold · ${Money.rupiah(totals.earned)}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Fresh.GreenDeep,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "This month: ${totals.monthItems} · ${Money.rupiah(totals.monthEarned)}",
            style = MaterialTheme.typography.bodyMedium,
            color = Fresh.GreenDeep,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SaleRow(sale: Sale, onOpen: () -> Unit, onLongPress: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Fresh.Card)
            .combinedClickable(onClickLabel = "Open plant", onLongClickLabel = "Change sale", onClick = onOpen, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(Sales.line(sale), style = MaterialTheme.typography.titleMedium, color = Fresh.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val sub = listOfNotNull(sale.kind.label, sale.soldOn.format(soldFormat), sale.note.takeIf { it.isNotBlank() }).joinToString(" · ")
        Text(sub, style = Small, color = Fresh.InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = Fresh.InkSoft.copy(alpha = 0.75f))
}
