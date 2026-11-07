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
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
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
        composable(Destinations.DASHBOARD) { entry ->
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
                onOpenHabit = { entry.ifResumed { nav.go(Destinations.TODAY) } },
                onOpenPlants = { entry.ifResumed { nav.go(Destinations.PLANTS) } },
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

        composable(Destinations.PLANTS) { entry ->
            val vm: PlantsViewModel = viewModel(
                factory = viewModelFactory { initializer { PlantsViewModel(ServiceLocator.plantRepository(context)) } },
            )
            PlantsScreen(
                viewModel = vm,
                onBack = { entry.ifResumed { nav.popBackStack() } },
                onCalendar = { entry.ifResumed { nav.go(Destinations.CALENDAR) } },
                onAdd = { entry.ifResumed { nav.go(Destinations.edit(0)) } },
                onEdit = { id -> entry.ifResumed { nav.go(Destinations.edit(id)) } },
            )
        }

        composable(Destinations.CALENDAR) { entry ->
            val vm: CalendarViewModel = viewModel(
                factory = viewModelFactory { initializer { CalendarViewModel(ServiceLocator.plantRepository(context)) } },
            )
            CalendarScreen(viewModel = vm, onBack = { entry.ifResumed { nav.popBackStack() } })
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
            EditPlantScreen(viewModel = vm, onDone = { entry.ifResumed { nav.popBackStack() } })
        }
    }

    var routed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!routed) {
            routed = true
            if (start != null) nav.go(start)
        }
    }
}

/**
 * Runs [block] only while this screen is the one in front. Navigating or popping moves the
 * screen out of RESUMED at once, so a double tap cannot pop twice (leaving a blank screen
 * with no destination) or push the same screen twice.
 */
private inline fun NavBackStackEntry.ifResumed(block: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) block()
}

private fun NavHostController.go(route: String) = navigate(route) { launchSingleTop = true }
