package dev.zakyymuh.motor

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.zakyymuh.motor.ui.ServiceEditScreen
import dev.zakyymuh.motor.ui.ServiceEditViewModel
import dev.zakyymuh.motor.ui.ServiceListScreen
import dev.zakyymuh.motor.ui.ServiceListViewModel
import dev.zakyymuh.timebox.ui.common.go
import dev.zakyymuh.timebox.ui.common.ifResumed

private const val LIST = "service"
private const val EDIT = "service/edit/{id}"

private fun edit(id: Long) = "service/edit/$id"

/** The service list, and the screen to log or edit one. Back on the list closes the app. */
@Composable
fun MotorNav(onClose: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val nav = rememberNavController()

    // The same short slide as papacti.
    NavHost(
        navController = nav,
        startDestination = LIST,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SLIDE_MS)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SLIDE_MS)) { it / 4 } },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(SLIDE_MS)) { it / 4 } },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(SLIDE_MS)) },
    ) {
        composable(LIST) { entry ->
            val vm: ServiceListViewModel = viewModel(
                factory = viewModelFactory { initializer { ServiceListViewModel(ServiceLocator.serviceRepository(context)) } },
            )
            ServiceListScreen(
                viewModel = vm,
                onBack = { entry.ifResumed(onClose) },
                onAdd = { entry.ifResumed { nav.go(edit(0)) } },
                onEdit = { id -> entry.ifResumed { nav.go(edit(id)) } },
            )
        }

        composable(
            route = EDIT,
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
    }
}

private const val SLIDE_MS = 280
