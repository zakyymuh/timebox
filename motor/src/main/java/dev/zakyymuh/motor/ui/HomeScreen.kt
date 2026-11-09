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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.motor.data.PhotoStore
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.theme.Fresh

/** The first screen: the dates that run out, then your vehicles. Back here closes the app. */
@Composable
fun HomeScreen(
    vehicles: VehicleListViewModel,
    expiries: ExpiryListViewModel,
    photos: PhotoStore,
    onBack: () -> Unit,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (Long) -> Unit,
    onAddExpiry: () -> Unit,
    onEditExpiry: (Long) -> Unit,
) {
    val cards by vehicles.cards.collectAsStateWithLifecycle()
    val rows by expiries.rows.collectAsStateWithLifecycle()
    val renewal by expiries.renewal.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // One tap renews, so one tap takes it back.
    LaunchedEffect(renewal) {
        val last = renewal ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(last.message(), actionLabel = "Undo", duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) expiries.undoRenewal() else expiries.dismissRenewal()
    }

    Scaffold(containerColor = Fresh.Page, snackbarHost = { SnackbarHost(snackbar) }) { insets ->
        Column(
            Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            TopBar(title = "Motor", onBack = onBack)
            val allCards = cards ?: return@Column
            val allRows = rows ?: return@Column
            ExpirySection(allRows, expiries.today, onAdd = onAddExpiry, onEdit = onEditExpiry, onRenew = expiries::renew)
            VehicleSection(allCards, vehicles.today, photos, onAdd = onAddVehicle, onOpen = onOpenVehicle)
            Spacer(Modifier.height(24.dp))
        }
    }
}
