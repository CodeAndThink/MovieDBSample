package com.truongngo.moviedb

import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.truongngo.moviedb.presenter.navigation.AppDestination
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the real NavControllers without creating a Firebase account. */
@RunWith(AndroidJUnit4::class)
class NavigationInstrumentedTest {
    private fun idle() = InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    private fun root(activity: MainActivity) =
        (activity.supportFragmentManager.findFragmentById(R.id.main_container) as NavHostFragment).navController

    private fun child(activity: MainActivity): androidx.navigation.NavController {
        val host = activity.supportFragmentManager.findFragmentById(R.id.main_container) as NavHostFragment
        val main = host.childFragmentManager.primaryNavigationFragment!!
        return (main.childFragmentManager.findFragmentById(R.id.main_fragment_container) as NavHostFragment).navController
    }

    @Test fun detailIsRootDestinationWithoutBottomNavigationAndReturnsToPreviousTab() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idle()
            scenario.onActivity { it.navigator.resetToLogin() }
            idle()
            scenario.onActivity {
                it.navigator.navigate(com.truongngo.moviedb.presenter.navigation.DeepLinkService().resolve("moviedb://app/detail/42")!!)
            }
            idle()
            scenario.onActivity {
                val controller = root(it)
                assertEquals(R.id.detail, controller.currentDestination?.id)
                assertEquals(R.id.app_navigation, controller.currentDestination?.parent?.id)
                assertEquals(42, controller.currentBackStackEntry?.arguments?.getInt("movieId"))
                assertEquals(R.id.main_screen, controller.previousBackStackEntry?.destination?.id)
                assertFalse(it.findViewById<android.view.View>(R.id.bottom_navigation)?.isShown == true)
                val entry = controller.currentBackStackEntry
                it.navigator.navigate(AppDestination.Detail(42))
                assertSame(entry, controller.currentBackStackEntry)
                it.navigator.navigate(AppDestination.Detail(43))
                assertEquals(43, controller.currentBackStackEntry?.arguments?.getInt("movieId"))
                assertTrue(it.navigator.back())
                assertEquals(R.id.main_screen, controller.currentDestination?.id)
            }
            idle()
            scenario.onActivity {
                assertEquals(R.id.home, child(it).currentDestination?.id)
                assertTrue(it.findViewById<android.view.View>(R.id.bottom_navigation).isShown)
                it.navigator.navigate(AppDestination.SETTINGS)
            }
            idle()
            scenario.onActivity { it.navigator.navigate(AppDestination.Detail(44)) }
            idle()
            scenario.onActivity {
                assertEquals(R.id.detail, root(it).currentDestination?.id)
                assertFalse(it.findViewById<android.view.View>(R.id.bottom_navigation)?.isShown == true)
                assertTrue(it.navigator.back())
            }
            idle()
            scenario.onActivity {
                assertEquals(R.id.settings, child(it).currentDestination?.id)
                it.navigator.navigate(AppDestination.Detail(45))
                it.navigator.navigate(AppDestination.HOME)
            }
            idle()
            scenario.onActivity { assertEquals(R.id.home, child(it).currentDestination?.id) }
        }
    }

    @Test fun searchKeepsQueryWhenReturningFromDetail() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idle()
            scenario.onActivity { it.navigator.navigate(AppDestination.SEARCH) }
            idle()
            scenario.onActivity {
                assertEquals(R.id.search, root(it).currentDestination?.id)
                assertFalse(it.findViewById<android.view.View>(R.id.bottom_navigation)?.isShown == true)
                it.findViewById<android.widget.EditText>(R.id.query).setText("Batman")
                it.navigator.navigate(AppDestination.Detail(42))
            }
            idle()
            scenario.onActivity {
                assertEquals(R.id.search, root(it).previousBackStackEntry?.destination?.id)
                assertTrue(it.navigator.back())
            }
            idle()
            scenario.onActivity {
                assertEquals(R.id.search, root(it).currentDestination?.id)
                assertEquals("Batman", it.findViewById<android.widget.EditText>(R.id.query).text.toString())
                assertTrue(it.navigator.back())
                assertEquals(R.id.main_screen, root(it).currentDestination?.id)
            }
        }
    }

    @Test fun signupIsSingleTopAndBackReturnsToLoginAfterRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idle()
            scenario.onActivity { it.navigator.resetToLogin() }
            idle()
            scenario.onActivity {
                it.navigator.navigate(AppDestination.SIGNUP)
                it.navigator.navigate(AppDestination.SIGNUP)
                assertEquals(R.id.signup, root(it).currentDestination?.id)
            }
            idle()
            scenario.recreate()
            idle()
            scenario.onActivity {
                assertEquals(R.id.signup, root(it).currentDestination?.id)
                assertTrue(it.navigator.back())
                assertEquals(R.id.login, root(it).currentDestination?.id)
            }
        }
    }

    @Test fun enteringMainClearsAuthStackAndSettingsBackGoesToHome() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idle()
            scenario.onActivity { it.navigator.resetToLogin() }
            idle()
            scenario.onActivity { it.navigator.navigate(AppDestination.SIGNUP) }
            idle()
            scenario.onActivity { it.navigator.navigate(AppDestination.SETTINGS) }
            idle()
            scenario.onActivity {
                val rootHost = it.supportFragmentManager.findFragmentById(R.id.main_container) as NavHostFragment
                assertEquals(R.id.main_screen, root(it).currentDestination?.id)
                val main = rootHost.childFragmentManager.primaryNavigationFragment!!
                val child = (main.childFragmentManager.findFragmentById(R.id.main_fragment_container) as NavHostFragment).navController
                assertEquals(R.id.main_screen, root(it).currentDestination?.id)
                assertNull(root(it).previousBackStackEntry)
                assertEquals(R.id.settings, child.currentDestination?.id)
                it.navigator.navigate(AppDestination.SETTINGS)
                assertTrue(it.navigator.back())
                assertEquals(R.id.home, child.currentDestination?.id)
                it.navigator.resetToLogin()
                assertEquals(R.id.login, root(it).currentDestination?.id)
                assertNull(root(it).previousBackStackEntry)
            }
        }
    }
}
