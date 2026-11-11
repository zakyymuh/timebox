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
import dev.zakyymuh.timebox.ui.breeding.BreedingDetailScreen
import dev.zakyymuh.timebox.ui.breeding.BreedingDetailViewModel
import dev.zakyymuh.timebox.ui.breeding.BreedingListScreen
import dev.zakyymuh.timebox.ui.breeding.BreedingListViewModel
import dev.zakyymuh.timebox.ui.breeding.BreedingNewScreen
import dev.zakyymuh.timebox.ui.breeding.BreedingNewViewModel
import dev.zakyymuh.timebox.ui.soil.AirSpaceScreen
import dev.zakyymuh.timebox.ui.soil.AirSpaceViewModel
import dev.zakyymuh.timebox.ui.soil.SoilStatusViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zakyymuh.timebox.ui.dashboard.DashboardViewModel
import dev.zakyymuh.timebox.ui.history.HistoryScreen
import dev.zakyymuh.timebox.ui.history.HistoryViewModel
import dev.zakyymuh.timebox.ui.encyclopedia.EncyclopediaScreen
import dev.zakyymuh.timebox.ui.encyclopedia.EntryScreen
import dev.zakyymuh.timebox.ui.plants.CalendarScreen
import dev.zakyymuh.timebox.ui.plants.CalendarViewModel
import dev.zakyymuh.timebox.ui.plants.EditPlantScreen
import dev.zakyymuh.timebox.ui.plants.EditPlantViewModel
import dev.zakyymuh.timebox.ui.plants.PlantsScreen
import dev.zakyymuh.timebox.ui.plants.PlantsViewModel

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
                            plants = ServiceLocator.plantRepository(context),
                            dex = ServiceLocator.dexRepository(context),
                            clock = ServiceLocator.clock,
                        )
                    }
                },
            )
            val soilVm: SoilStatusViewModel = viewModel(
                factory = viewModelFactory { initializer { SoilStatusViewModel(ServiceLocator.soilRepository(context)) } },
            )
            val soil by soilVm.status.collectAsStateWithLifecycle()
            val breedingVm: BreedingListViewModel = viewModel(
                factory = viewModelFactory { initializer { BreedingListViewModel(ServiceLocator.breedingRepository(context)) } },
            )
            val crosses by breedingVm.crosses.collectAsStateWithLifecycle()
            DashboardScreen(
                viewModel = vm,
                soil = soil,
                onOpenSoil = { entry.ifResumed { nav.go(Destinations.SOIL) } },
                crosses = crosses,
                onOpenBreeding = { entry.ifResumed { nav.go(Destinations.BREEDING) } },
                onOpenPlants = { entry.ifResumed { nav.go(Destinations.PLANTS) } },
                onOpenCollection = { entry.ifResumed { nav.go(Destinations.COLLECTION) } },
                onOpenHistory = { entry.ifResumed { nav.go(Destinations.HISTORY) } },
            )
        }

        composable(Destinations.BREEDING) { entry ->
            val vm: BreedingListViewModel = viewModel(
                factory = viewModelFactory { initializer { BreedingListViewModel(ServiceLocator.breedingRepository(context)) } },
            )
            BreedingListScreen(
                viewModel = vm,
                onBack = { entry.ifResumed { nav.popBackStack() } },
                onOpen = { id -> entry.ifResumed { nav.go(Destinations.cross(id)) } },
                onAdd = { entry.ifResumed { nav.go(Destinations.BREEDING_NEW) } },
            )
        }

        composable(Destinations.BREEDING_NEW) { entry ->
            val vm: BreedingNewViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { BreedingNewViewModel(ServiceLocator.breedingRepository(context), ServiceLocator.dexRepository(context)) }
                },
            )
            BreedingNewScreen(
                viewModel = vm,
                onBack = { entry.ifResumed { nav.popBackStack() } },
                // Show the new cross, with the list underneath for Back.
                onSaved = { id ->
                    entry.ifResumed {
                        nav.navigate(Destinations.cross(id)) { popUpTo(Destinations.BREEDING) }
                    }
                },
            )
        }

        composable(
            route = Destinations.CROSS,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val vm: BreedingDetailViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        BreedingDetailViewModel(ServiceLocator.breedingRepository(context), ServiceLocator.dexRepository(context), id)
                    }
                },
            )
            BreedingDetailScreen(
                viewModel = vm,
                onBack = { entry.ifResumed { nav.popBackStack() } },
                onOpenPlant = { plant -> entry.ifResumed { nav.go(Destinations.dex(plant)) } },
            )
        }

        composable(Destinations.HISTORY) { entry ->
            val vm: HistoryViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        HistoryViewModel(
                            plants = ServiceLocator.plantRepository(context),
                            dex = ServiceLocator.dexRepository(context),
                            notes = ServiceLocator.noteRepository(context),
                            clock = ServiceLocator.clock,
                        )
                    }
                },
            )
            HistoryScreen(viewModel = vm, onBack = { entry.ifResumed { nav.popBackStack() } })
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
                onEncyclopedia = { entry.ifResumed { nav.go(Destinations.ENCYCLOPEDIA) } },
            )
        }

        composable(Destinations.SOIL) { entry ->
            val vm: AirSpaceViewModel = viewModel(
                factory = viewModelFactory { initializer { AirSpaceViewModel(ServiceLocator.soilRepository(context)) } },
            )
            AirSpaceScreen(viewModel = vm, onBack = { entry.ifResumed { nav.popBackStack() } })
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
            DexEditScreen(
                viewModel = vm,
                onDone = { entry.ifResumed { nav.popBackStack() } },
                // Straight to the list: the plant's page is gone, and popping to it first left
                // it blank (it could not close itself while still coming back into view).
                onDeleted = {
                    entry.ifResumed {
                        if (!nav.popBackStack(Destinations.COLLECTION, inclusive = false)) nav.go(Destinations.COLLECTION)
                    }
                },
            )
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
