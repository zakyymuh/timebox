package dev.zakyymuh.timebox.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.zakyymuh.timebox.ServiceLocator
import dev.zakyymuh.timebox.ui.dashboard.DashboardScreen
import dev.zakyymuh.timebox.ui.dashboard.DashboardViewModel
import dev.zakyymuh.timebox.ui.plants.CalendarScreen
import dev.zakyymuh.timebox.ui.plants.CalendarViewModel
import dev.zakyymuh.timebox.ui.plants.EditPlantScreen
import dev.zakyymuh.timebox.ui.plants.EditPlantViewModel
import dev.zakyymuh.timebox.ui.plants.PlantsScreen
import dev.zakyymuh.timebox.ui.plants.PlantsViewModel
import dev.zakyymuh.timebox.ui.today.TodayScreen
import dev.zakyymuh.timebox.ui.today.TodayViewModel

/**
 * The app's screens. Always starts on the dashboard; when a notification or the widget
 * asked for a particular screen ([start]), it is opened on top once, so Back returns to
 * the dashboard rather than leaving the app.
 */
@Composable
fun AppNav(start: String?, sendTestReminder: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Destinations.DASHBOARD) {
        composable(Destinations.DASHBOARD) {
            val vm: DashboardViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        DashboardViewModel(
                            timebox = ServiceLocator.repository(context),
                            plants = ServiceLocator.plantRepository(context),
                        )
                    }
                },
            )
            DashboardScreen(
                viewModel = vm,
                onOpenHabit = { nav.navigate(Destinations.TODAY) },
                onOpenPlants = { nav.navigate(Destinations.PLANTS) },
            )
        }

        composable(Destinations.TODAY) {
            val vm: TodayViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        TodayViewModel(repo = ServiceLocator.repository(context), sendTestReminder = sendTestReminder)
                    }
                },
            )
            TodayScreen(viewModel = vm)
        }

        composable(Destinations.PLANTS) {
            val vm: PlantsViewModel = viewModel(
                factory = viewModelFactory { initializer { PlantsViewModel(ServiceLocator.plantRepository(context)) } },
            )
            PlantsScreen(
                viewModel = vm,
                onBack = { nav.popBackStack() },
                onCalendar = { nav.navigate(Destinations.CALENDAR) },
                onAdd = { nav.navigate(Destinations.edit(0)) },
                onEdit = { id -> nav.navigate(Destinations.edit(id)) },
            )
        }

        composable(Destinations.CALENDAR) {
            val vm: CalendarViewModel = viewModel(
                factory = viewModelFactory { initializer { CalendarViewModel(ServiceLocator.plantRepository(context)) } },
            )
            CalendarScreen(viewModel = vm, onBack = { nav.popBackStack() })
        }

        composable(
            route = Destinations.EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val vm: EditPlantViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { EditPlantViewModel(ServiceLocator.plantRepository(context), id) }
                },
            )
            EditPlantScreen(viewModel = vm, onDone = { nav.popBackStack() })
        }
    }

    var routed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!routed) {
            routed = true
            if (start != null) nav.navigate(start)
        }
    }
}
