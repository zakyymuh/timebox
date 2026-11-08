package dev.zakyymuh.timebox.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
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
import dev.zakyymuh.timebox.ui.common.go
import dev.zakyymuh.timebox.ui.common.ifResumed
import dev.zakyymuh.timebox.ui.collection.CollectionScreen
import dev.zakyymuh.timebox.ui.collection.CollectionViewModel
import dev.zakyymuh.timebox.ui.collection.DexDetailScreen
import dev.zakyymuh.timebox.ui.collection.DexDetailViewModel
import dev.zakyymuh.timebox.ui.collection.DexEditScreen
import dev.zakyymuh.timebox.ui.collection.DexEditViewModel
import dev.zakyymuh.timebox.ui.dashboard.DashboardScreen
import dev.zakyymuh.timebox.ui.soil.AirSpaceScreen
import dev.zakyymuh.timebox.ui.soil.AirSpaceViewModel
import dev.zakyymuh.timebox.ui.soil.SoilStatusViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.ui.motor.ServiceEditScreen
import dev.zakyymuh.timebox.ui.motor.ServiceEditViewModel
import dev.zakyymuh.timebox.ui.motor.ServiceListScreen
import dev.zakyymuh.timebox.ui.motor.ServiceListViewModel
import dev.zakyymuh.timebox.ui.dashboard.DashboardViewModel
import dev.zakyymuh.timebox.ui.encyclopedia.EncyclopediaScreen
import dev.zakyymuh.timebox.ui.encyclopedia.EntryScreen
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
fun AppNav(start: String?) {
    val context = LocalContext.current.applicationContext
    val nav = rememberNavController()

    // A short horizontal slide, not the library's default 700ms crossfade. A back swipe
    // scrubs this animation with the finger: a crossfade shows two white screens through
    // each other, a slide moves the page away and reveals the one underneath.
    NavHost(
        navController = nav,
        startDestination = Destinations.DASHBOARD,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SLIDE_MS))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SLIDE_MS)) { it / 4 }
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(SLIDE_MS)) { it / 4 }
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(SLIDE_MS))
        },
    ) {
        composable(Destinations.DASHBOARD) { entry ->
            val vm: DashboardViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        DashboardViewModel(
                            timebox = ServiceLocator.repository(context),
                            plants = ServiceLocator.plantRepository(context),
                            dex = ServiceLocator.dexRepository(context),
                            service = ServiceLocator.serviceRepository(context),
                        )
                    }
                },
            )
            DashboardScreen(
                viewModel = vm,
                onOpenHabit = { entry.ifResumed { nav.go(Destinations.TODAY) } },
                onOpenPlants = { entry.ifResumed { nav.go(Destinations.PLANTS) } },
                onOpenCollection = { entry.ifResumed { nav.go(Destinations.COLLECTION) } },
                onOpenService = { entry.ifResumed { nav.go(Destinations.SERVICE) } },
                onOpenEntry = { id -> entry.ifResumed { nav.go(Destinations.entry(id)) } },
                onOpenEncyclopedia = { entry.ifResumed { nav.go(Destinations.ENCYCLOPEDIA) } },
            )
        }

        composable(Destinations.TODAY) {
            val vm: TodayViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        TodayViewModel(repo = ServiceLocator.repository(context))
                    }
                },
            )
            TodayScreen(viewModel = vm)
        }

        composable(Destinations.PLANTS) { entry ->
            val vm: PlantsViewModel = viewModel(
                factory = viewModelFactory { initializer { PlantsViewModel(ServiceLocator.plantRepository(context)) } },
            )
            val soilVm: SoilStatusViewModel = viewModel(
                factory = viewModelFactory { initializer { SoilStatusViewModel(ServiceLocator.soilRepository(context)) } },
            )
            val soil by soilVm.status.collectAsStateWithLifecycle()
            PlantsScreen(
                viewModel = vm,
                onBack = { entry.ifResumed { nav.popBackStack() } },
                onCalendar = { entry.ifResumed { nav.go(Destinations.CALENDAR) } },
                onAdd = { entry.ifResumed { nav.go(Destinations.edit(0)) } },
                onEdit = { id -> entry.ifResumed { nav.go(Destinations.edit(id)) } },
                onEncyclopedia = { entry.ifResumed { nav.go(Destinations.ENCYCLOPEDIA) } },
                soil = soil,
                onSoil = { entry.ifResumed { nav.go(Destinations.SOIL) } },
            )
        }

        composable(Destinations.SOIL) { entry ->
            val vm: AirSpaceViewModel = viewModel(
                factory = viewModelFactory { initializer { AirSpaceViewModel(ServiceLocator.soilRepository(context)) } },
            )
            AirSpaceScreen(viewModel = vm, onBack = { entry.ifResumed { nav.popBackStack() } })
        }

        composable(Destinations.SERVICE) { entry ->
            val vm: ServiceListViewModel = viewModel(
                factory = viewModelFactory { initializer { ServiceListViewModel(ServiceLocator.serviceRepository(context)) } },
            )
            ServiceListScreen(
                viewModel = vm,
                onBack = { entry.ifResumed { nav.popBackStack() } },
                onAdd = { entry.ifResumed { nav.go(Destinations.serviceEdit(0)) } },
                onEdit = { id -> entry.ifResumed { nav.go(Destinations.serviceEdit(id)) } },
            )
        }

        composable(
            route = Destinations.SERVICE_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val vm: ServiceEditViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ServiceEditViewModel(ServiceLocator.serviceRepository(context), id) }
                },
            )
            ServiceEditScreen(viewModel = vm, onDone = { entry.ifResumed { nav.popBackStack() } })
        }

        composable(Destinations.COLLECTION) { entry ->
            val vm: CollectionViewModel = viewModel(
                factory = viewModelFactory { initializer { CollectionViewModel(ServiceLocator.dexRepository(context)) } },
            )
            CollectionScreen(
                viewModel = vm,
                onBack = { entry.ifResumed { nav.popBackStack() } },
                onOpen = { id -> entry.ifResumed { nav.go(Destinations.dex(id)) } },
                onAdd = { owned -> entry.ifResumed { nav.go(Destinations.dexEdit(0, owned)) } },
            )
        }

        composable(
            route = Destinations.DEX,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val vm: DexDetailViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        DexDetailViewModel(ServiceLocator.dexRepository(context), ServiceLocator.photoStore(context), id)
                    }
                },
            )
            DexDetailScreen(
                viewModel = vm,
                onBack = { entry.ifResumed { nav.popBackStack() } },
                onEdit = { entry.ifResumed { nav.go(Destinations.dexEdit(id, owned = true)) } },
                onOpenEntry = { info -> entry.ifResumed { nav.go(Destinations.entry(info)) } },
            )
        }

        composable(
            route = Destinations.DEX_EDIT,
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("owned") { type = NavType.BoolType },
            ),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val owned = entry.arguments?.getBoolean("owned") ?: true
            val vm: DexEditViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        DexEditViewModel(ServiceLocator.dexRepository(context), ServiceLocator.photoStore(context), id, owned)
                    }
                },
            )
            DexEditScreen(viewModel = vm, onDone = { entry.ifResumed { nav.popBackStack() } })
        }

        composable(Destinations.ENCYCLOPEDIA) { entry ->
            EncyclopediaScreen(
                onBack = { entry.ifResumed { nav.popBackStack() } },
                onOpen = { id -> entry.ifResumed { nav.go(Destinations.entry(id)) } },
            )
        }

        composable(
            route = Destinations.ENTRY,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            EntryScreen(
                id = entry.arguments?.getString("id").orEmpty(),
                onBack = { entry.ifResumed { nav.popBackStack() } },
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

private const val SLIDE_MS = 280
