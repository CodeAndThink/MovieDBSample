package com.truongngo.moviedb.presenter.navigation

import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.navOptions
import com.truongngo.moviedb.R
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

/** Owned by an Activity. Controllers are explicitly detached with their host lifecycle. */
@ActivityScoped
class AppNavigator @Inject constructor() {
    private companion object { const val MAIN_TARGET = "main_target" }

    private var root: NavController? = null
    private var main: NavController? = null

    fun attachRoot(controller: NavController) { root = controller; consumeMainTarget() }
    fun detachRoot() { root = null; main = null }
    fun attachMain(controller: NavController) {
        main = controller
        consumeMainTarget()
    }
    fun detachMain(controller: NavController) {
        if (main === controller) main = null
    }

    fun navigate(destination: AppDestination) {
        val controller = checkNotNull(root)
        when (destination) {
            AppDestination.LOGIN -> backToLogin()
            AppDestination.SIGNUP -> if (controller.currentDestination?.id != R.id.signup) {
                controller.navigate(R.id.signup, null, navOptions {
                    if (controller.currentDestination?.id == R.id.splash) {
                        popUpTo(R.id.splash) { inclusive = true }
                    }
                    launchSingleTop = true
                })
            }
            AppDestination.SEARCH -> {
                if (controller.currentDestination?.id == R.id.search) return
                if (!controller.popBackStack(R.id.search, false)) {
                    ensureMain()
                    controller.navigate(R.id.search)
                }
            }
            is AppDestination.Detail -> {
                val current = controller.currentBackStackEntry
                if (current?.destination?.id == R.id.detail && current.arguments?.getInt("movieId") == destination.movieId) return
                if (controller.currentDestination?.id == R.id.detail) controller.popBackStack()
                if (controller.currentDestination?.id !in setOf(R.id.main_screen, R.id.search)) ensureMain()
                controller.navigate(R.id.detail, bundleOf("movieId" to destination.movieId), navOptions {
                    launchSingleTop = true
                })
            }
            AppDestination.HOME, AppDestination.SETTINGS -> {
                ensureMain()
                controller.getBackStackEntry(R.id.main_screen).savedStateHandle[MAIN_TARGET] = destination.encode()
                consumeMainTarget()
            }
        }
    }

    private fun ensureMain() {
        val controller = checkNotNull(root)
        if (controller.currentDestination?.id == R.id.main_screen) return
        if (controller.popBackStack(R.id.main_screen, false)) return
        main = null
        controller.navigate(R.id.main_screen, null, navOptions {
            popUpTo(controller.graph.id) { inclusive = true }
            launchSingleTop = true
        })
    }

    private fun consumeMainTarget() {
        val controller = main ?: return
        val rootController = root ?: return
        if (rootController.currentDestination?.id != R.id.main_screen) return
        val state = rootController.getBackStackEntry(R.id.main_screen).savedStateHandle
        val target = state.get<String>(MAIN_TARGET) ?: return
        
        val destination = AppDestination.decode(target)
        val targetId = if (destination == AppDestination.SETTINGS) R.id.settings_graph else R.id.home_graph
        
        if (controller.currentDestination?.id != targetId && controller.currentDestination?.parent?.id != targetId) {
            controller.navigate(targetId, null, navOptions {
                popUpTo(controller.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            })
        }
        if (destination == AppDestination.HOME) controller.popBackStack(R.id.home, false)
        state[MAIN_TARGET] = null
    }

    fun back(): Boolean {
        // Root destinations must be popped before touching Main's retained child stack.
        if (root?.currentDestination?.id != R.id.main_screen) return root?.popBackStack() == true
        if (main?.currentDestination?.id == R.id.settings) {
            navigate(AppDestination.HOME)
            return true
        }
        return main?.popBackStack() == true || root?.popBackStack() == true
    }

    fun backToLogin() {
        val controller = checkNotNull(root)
        if (controller.currentDestination?.id == R.id.login) return
        if (!controller.popBackStack(R.id.login, false)) resetToLogin()
    }

    fun resetToLogin() {
        main = null
        val controller = checkNotNull(root)
        controller.navigate(R.id.login, null, navOptions {
            popUpTo(controller.graph.id) { inclusive = true }
            launchSingleTop = true
        })
    }
}
