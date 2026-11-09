package dev.zakyymuh.motor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.motor.data.PhotoStore
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.theme.Fresh

/** The first screen: your vehicles. Back here closes the app. */
@Composable
fun HomeScreen(
    vehicles: VehicleListViewModel,
    photos: PhotoStore,
    onBack: () -> Unit,
    onAddVehicle: () -> Unit,
    onOpenVehicle: (Long) -> Unit,
) {
    val cards by vehicles.cards.collectAsStateWithLifecycle()
    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(
            Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            TopBar(title = "Motor", onBack = onBack)
            val all = cards ?: return@Column
            VehicleSection(all, vehicles.today, photos, onAdd = onAddVehicle, onOpen = onOpenVehicle)
            Spacer(Modifier.height(24.dp))
        }
    }
}
