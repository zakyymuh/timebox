package dev.zakyymuh.motor

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.zakyymuh.motor.ui.ExpiryEditScreen
import dev.zakyymuh.motor.ui.ExpiryEditViewModel
import dev.zakyymuh.motor.ui.ExpiryListViewModel
import dev.zakyymuh.motor.ui.HomeScreen
import dev.zakyymuh.motor.ui.LastTimeViewModel
import dev.zakyymuh.motor.ui.ServiceEditScreen
import dev.zakyymuh.motor.ui.ServiceEditViewModel
import dev.zakyymuh.motor.ui.ServiceListScreen
import dev.zakyymuh.motor.ui.ServiceListViewModel
import dev.zakyymuh.motor.ui.VehicleEditScreen
import dev.zakyymuh.motor.ui.VehicleEditViewModel
import dev.zakyymuh.motor.ui.VehicleListViewModel
import dev.zakyymuh.timebox.ui.common.go
import dev.zakyymuh.timebox.ui.common.ifResumed

private const val HOME = "home"
private const val VEHICLE = "vehicle/{vehicleId}"
private const val VEHICLE_EDIT = "vehicle/edit/{id}"
private const val SERVICE_EDIT = "service/edit/{vehicleId}/{id}"
private const val EXPIRY_EDIT = "expiry/edit/{id}"

private fun vehicle(id: Long) = "vehicle/$id"
private fun vehicleEdit(id: Long) = "vehicle/edit/$id"
private fun serviceEdit(vehicleId: Long, id: Long) = "service/edit/$vehicleId/$id"
private fun expiryEdit(id: Long) = "expiry/edit/$id"

private fun NavBackStackEntry.long(name: String): Long = arguments?.getLong(name) ?: 0L

/**
 * Home lists the dates that run out and your vehicles; a vehicle opens its service log.
 * Back on home closes the app.
 */
@Composable
fun MotorNav(onClose: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val nav = rememberNavController()
    val photos = remember { ServiceLocator.photoStore(context) }

    // The same short slide as papacti.
    NavHost(
        navController = nav,
        startDestination = HOME,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SLIDE_MS)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SLIDE_MS)) { it / 4 } },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(SLIDE_MS)) { it / 4 } },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(SLIDE_MS)) },
    ) {
        composable(HOME) { entry ->
            val vehicles: VehicleListViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        VehicleListViewModel(ServiceLocator.vehicleRepository(context), ServiceLocator.serviceRepository(context))
                    }
                },
            )
            val expiries: ExpiryListViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        ExpiryListViewModel(ServiceLocator.expiryRepository(context), ServiceLocator.vehicleRepository(context))
                    }
                },
            )
            val lastTime: LastTimeViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        LastTimeViewModel(ServiceLocator.markRepository(context), ServiceLocator.vehicleRepository(context))
                    }
                },
            )
            HomeScreen(
                vehicles = vehicles,
                expiries = expiries,
                lastTime = lastTime,
                photos = photos,
                onBack = { entry.ifResumed(onClose) },
                onAddVehicle = { entry.ifResumed { nav.go(vehicleEdit(0)) } },
                onOpenVehicle = { id -> entry.ifResumed { nav.go(vehicle(id)) } },
                onAddExpiry = { entry.ifResumed { nav.go(expiryEdit(0)) } },
                onEditExpiry = { id -> entry.ifResumed { nav.go(expiryEdit(id)) } },
            )
        }

        composable(VEHICLE, arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })) { entry ->
            val vehicleId = entry.long("vehicleId")
            val vm: ServiceListViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        ServiceListViewModel(ServiceLocator.serviceRepository(context), ServiceLocator.vehicleRepository(context), vehicleId)
                    }
                },
            )
            ServiceListScreen(
                viewModel = vm,
                onBack = { entry.ifResumed { nav.popBackStack() } },
                onEditVehicle = { entry.ifResumed { nav.go(vehicleEdit(vehicleId)) } },
                onAdd = { entry.ifResumed { nav.go(serviceEdit(vehicleId, 0)) } },
                onEdit = { id -> entry.ifResumed { nav.go(serviceEdit(vehicleId, id)) } },
            )
        }

        composable(VEHICLE_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.long("id")
            val vm: VehicleEditViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { VehicleEditViewModel(ServiceLocator.vehicleRepository(context), photos, id) }
                },
            )
            VehicleEditScreen(
                viewModel = vm,
                onDone = { entry.ifResumed { nav.popBackStack() } },
                // The vehicle's own screen is gone too, so go all the way home.
                onDeleted = { entry.ifResumed { nav.popBackStack(HOME, inclusive = false) } },
            )
        }

        composable(
            route = SERVICE_EDIT,
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("id") { type = NavType.LongType },
            ),
        ) { entry ->
            val vehicleId = entry.long("vehicleId")
            val id = entry.long("id")
            val vm: ServiceEditViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ServiceEditViewModel(ServiceLocator.serviceRepository(context), id, vehicleId) }
                },
            )
            ServiceEditScreen(viewModel = vm, onDone = { entry.ifResumed { nav.popBackStack() } })
        }

        composable(EXPIRY_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.long("id")
            val vm: ExpiryEditViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        ExpiryEditViewModel(ServiceLocator.expiryRepository(context), ServiceLocator.vehicleRepository(context), id)
                    }
                },
            )
            ExpiryEditScreen(viewModel = vm, onDone = { entry.ifResumed { nav.popBackStack() } })
        }
    }
}

private const val SLIDE_MS = 280
