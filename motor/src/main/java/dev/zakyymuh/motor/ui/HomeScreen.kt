package dev.zakyymuh.motor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.motor.data.PhotoStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import dev.zakyymuh.timebox.ui.theme.Label
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import dev.zakyymuh.timebox.ui.lastdone.FunFactCard
import dev.zakyymuh.timebox.ui.lastdone.LastTimeSheet
import dev.zakyymuh.timebox.ui.theme.Fresh

/**
 * The first screen: a greeting, your vehicles as big cards, a line about a chore, then
 * the dates that run out. Back here closes the app.
 */
@Composable
fun HomeScreen(
    vehicles: VehicleListViewModel,
    expiries: ExpiryListViewModel,
    lastTime: LastTimeViewModel,
    photos: PhotoStore,
    onBack: () -> Unit,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (Long) -> Unit,
    onEditVehicle: (Long) -> Unit,
    onAddExpiry: () -> Unit,
    onEditExpiry: (Long) -> Unit,
) {
    val cards by vehicles.cards.collectAsStateWithLifecycle()
    val rows by expiries.rows.collectAsStateWithLifecycle()
    val renewal by expiries.renewal.collectAsStateWithLifecycle()
    val lastDone by lastTime.lastDone.collectAsStateWithLifecycle()
    val choreVehicles by lastTime.vehicles.collectAsStateWithLifecycle()
    var lastTimeOpen by rememberSaveable { mutableStateOf(false) }
    // Which vehicle a typed chore is for; null is none. Unset, it is the first vehicle.
    var choreFor by rememberSaveable { mutableStateOf<Long?>(null) }
    var choreForPicked by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    // One tap renews, so one tap takes it back.
    LaunchedEffect(renewal) {
        val last = renewal ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(last.message(), actionLabel = "Undo", duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) expiries.undoRenewal() else expiries.dismissRenewal()
    }

    // No back button on home any more; system Back still closes the app.
    BackHandler(onBack = onBack)

    Scaffold(containerColor = Fresh.Page, snackbarHost = { SnackbarHost(snackbar) }) { insets ->
        Column(
            Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            Greeting(vehicles.today)
            val allCards = cards ?: return@Column
            val allRows = rows ?: return@Column
            VehicleSection(allCards, vehicles.today, photos, onAdd = onAddVehicle, onOpen = onOpenVehicle, onEdit = onEditVehicle)
            Spacer(Modifier.height(22.dp))
            FunFactCard(
                fact = lastDone.fact,
                tracked = lastDone.events.size,
                onShuffle = lastTime::shuffleFact,
                onOpen = { lastTimeOpen = true },
                intro = "Keep track of when you last did things — cuci motor, tambah angin, a SIM for the car.",
            )
            ExpirySection(allRows, expiries.today, onAdd = onAddExpiry, onEdit = onEditExpiry, onRenew = expiries::renew)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (lastTimeOpen) {
        val forVehicle = if (choreForPicked) choreFor?.takeIf { id -> choreVehicles.any { it.id == id } } else choreVehicles.firstOrNull()?.id
        LastTimeSheet(
            ui = lastDone,
            onDidIt = { name -> lastTime.markDone(name, forVehicle) },
            onAgain = lastTime::again,
            onUndo = lastTime::undoLastMark,
            onDismiss = { lastTimeOpen = false },
            prompt = "Cuci motor, tambah angin…",
        ) {
            ForVehicle(choreVehicles, forVehicle) { picked ->
                choreFor = picked
                choreForPicked = true
            }
        }
    }
}

private val greetingDate = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)

/** "Welcome back, Zaky", like Daily's "Hello Zaky", with today's date under it. */
@Composable
private fun Greeting(today: LocalDate) {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp)) {
        Text("Welcome back, $OWNER", style = MaterialTheme.typography.headlineLarge, color = Fresh.Ink)
        Text(today.format(greetingDate), style = Label, color = Fresh.InkSoft, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Whose app this is. One person's phone, so a constant, as in Daily. */
private const val OWNER = "Zaky"
