package com.truongngo.moviedb

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import com.truongngo.moviedb.presenter.home.HomeFragment
import com.truongngo.moviedb.presenter.home.HomeState
import com.truongngo.moviedb.presenter.home.HomeViewModel
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.ConcatAdapter
import com.truongngo.moviedb.presenter.home.HomeMovieAdapter
import com.truongngo.moviedb.presenter.home.HomeSeeMoreAdapter
import com.truongngo.moviedb.presenter.navigation.AppDestination
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** UI smoke test against the configured TMDB service; also accepts its visible error state. */
@RunWith(AndroidJUnit4::class)
class HomeInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun home(activity: MainActivity): HomeFragment {
        val root = activity.supportFragmentManager.findFragmentById(R.id.main_container) as NavHostFragment
        val main = root.childFragmentManager.primaryNavigationFragment!!
        val child = main.childFragmentManager.findFragmentById(R.id.main_fragment_container) as NavHostFragment
        return child.childFragmentManager.primaryNavigationFragment as HomeFragment
    }

    @Test fun homeRendersAndSurvivesViewRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.navigator.navigate(AppDestination.HOME) }
            instrumentation.waitForIdleSync()
            var state = HomeState(isRefreshing = true)
            var refreshing = true
            val deadline = SystemClock.elapsedRealtime() + 40_000
            while (refreshing && SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity {
                    val fragment = home(it)
                    state = ViewModelProvider(fragment.findNavController().getBackStackEntry(R.id.main_navigation))[HomeViewModel::class.java].stateFlow.value
                    refreshing = fragment.requireView().findViewById<SwipeRefreshLayout>(R.id.refresh).isRefreshing
                }
                SystemClock.sleep(100)
            }
            assertFalse("Banner and feed refreshes should complete or expose an error", refreshing)
            scenario.onActivity { activity ->
                val fragment = home(activity)
                val pager = fragment.requireView().findViewById<ViewPager2>(R.id.nowPlayingPager)
                assertTrue(state.nowPlaying.size <= 10)
                assertEquals(if (state.nowPlaying.size > 1) state.nowPlaying.size + 2 else state.nowPlaying.size,
                    pager.adapter!!.itemCount)
                listOf(R.id.popular, R.id.topRated, R.id.upcoming).forEach { sectionId ->
                    val section = fragment.requireView().findViewById<View>(sectionId)
                    val adapters = (section.findViewById<RecyclerView>(R.id.movies).adapter as ConcatAdapter).adapters
                    assertTrue(adapters.first() is HomeMovieAdapter)
                    assertTrue(adapters.first().itemCount <= 10)
                    assertTrue(adapters.last() is HomeSeeMoreAdapter)
                    assertEquals(1, adapters.last().itemCount)
                    assertTrue(section.findViewById<View>(R.id.seeMore).isShown)
                }

            }
            if (state.nowPlaying.size > 1) {
                scenario.onActivity {
                    home(it).requireView().findViewById<ViewPager2>(R.id.nowPlayingPager)
                        .setCurrentItem(state.nowPlaying.size + 1, true)
                }
                SystemClock.sleep(1000)
                instrumentation.waitForIdleSync()
                scenario.onActivity {
                    val pager = home(it).requireView().findViewById<ViewPager2>(R.id.nowPlayingPager)
                    assertEquals(1, pager.currentItem)
                    pager.setCurrentItem(0, true)
                }
                SystemClock.sleep(1000)
                instrumentation.waitForIdleSync()
                scenario.onActivity {
                    val pager = home(it).requireView().findViewById<ViewPager2>(R.id.nowPlayingPager)
                    assertEquals(state.nowPlaying.size, pager.currentItem)
                    pager.setCurrentItem(1, false)
                }
            }
            // Let asynchronous image requests paint before capturing the rendered screen.
            SystemClock.sleep(2000)
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            File(instrumentation.targetContext.getExternalFilesDir(null), "home-screen.png").outputStream().use {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            screenshot.recycle()
            scenario.onActivity {
                home(it).requireView().findViewById<NestedScrollView>(R.id.scroll).fullScroll(android.view.View.FOCUS_DOWN)
            }
            SystemClock.sleep(1000)
            val sectionsScreenshot = instrumentation.uiAutomation.takeScreenshot()
            File(instrumentation.targetContext.getExternalFilesDir(null), "home-sections.png").outputStream().use {
                sectionsScreenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            sectionsScreenshot.recycle()
            scenario.onActivity { home(it).requireView().findViewById<NestedScrollView>(R.id.scroll).scrollTo(0, 0) }
            instrumentation.waitForIdleSync()
            val version = state.refreshVersion
            onView(withId(R.id.refresh)).perform(swipeDown())
            val refreshDeadline = SystemClock.elapsedRealtime() + 40_000
            do {
                scenario.onActivity { state = ViewModelProvider(home(it).findNavController().getBackStackEntry(R.id.main_navigation))[HomeViewModel::class.java].stateFlow.value }
                SystemClock.sleep(100)
            } while (state.refreshVersion == version && SystemClock.elapsedRealtime() < refreshDeadline)
            assertTrue("Pull to refresh should reload Home", state.refreshVersion > version)
            // Recreate Home's view without involving the independent Firebase session guard.
            scenario.onActivity {
                val fragment = home(it)
                val manager = fragment.parentFragmentManager
                manager.beginTransaction().detach(fragment).commitNow()
                manager.beginTransaction().attach(fragment).setPrimaryNavigationFragment(fragment).commitNow()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                val restored = ViewModelProvider(home(it).findNavController().getBackStackEntry(R.id.main_navigation))[HomeViewModel::class.java].stateFlow.value
                assertEquals(state.nowPlaying, restored.nowPlaying)
                assertFalse(restored.isRefreshing)
            }
        }
    }
}
