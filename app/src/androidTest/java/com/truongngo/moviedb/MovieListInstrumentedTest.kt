package com.truongngo.moviedb

import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.truongngo.moviedb.data.local.database.HomeCacheDatabase
import com.truongngo.moviedb.data.local.model.HomeCacheKey
import com.truongngo.moviedb.data.paging.moviePage
import com.truongngo.moviedb.domain.model.HomeFeed
import com.truongngo.moviedb.presenter.home.HomeMovieAdapter
import com.truongngo.moviedb.presenter.home.HomeSeeMoreAdapter
import com.truongngo.moviedb.presenter.navigation.AppDestination
import com.truongngo.moviedb.presenter.navigation.NavigationViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MovieListInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun root(activity: MainActivity) =
        (activity.supportFragmentManager.findFragmentById(R.id.main_container) as NavHostFragment).navController
    private fun idle() = instrumentation.waitForIdleSync()

    private fun saveScreenshot(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        java.io.File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    @Test fun homePreviewAndMovieListPreserveFeedAndBackStack() = runBlocking {
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, android.Manifest.permission.POST_NOTIFICATIONS)
        val db = Room.databaseBuilder(context, HomeCacheDatabase::class.java, "home-cache.db").build()
        try {
            for (feed in listOf(HomeFeed.POPULAR, HomeFeed.TOP_RATED, HomeFeed.UPCOMING)) {
                val key = HomeCacheKey(feed.name)
                val previous = db.pagingDao().remoteKey(key.feed, key.language, key.region)
                db.pagingDao().savePage(key, moviePage(1, (1..20).toList(), total = 1), previous, System.currentTimeMillis())
            }
        } finally { db.close() }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { ViewModelProvider(it)[NavigationViewModel::class.java].notificationPermissionFinished() }
            SystemClock.sleep(800)
            idle()
            // Navigation is exercised directly; login/pending-route policy is covered by unit tests.
            scenario.onActivity { it.navigator.navigate(AppDestination.HOME) }
            idle()
            var previewCount = 0
            var savedTopRatedPosition = 0
            val deadline = SystemClock.elapsedRealtime() + 10_000
            while (previewCount != 10 && SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity {
                    val section = it.findViewById<View>(R.id.topRated)
                    val adapter = section.findViewById<RecyclerView>(R.id.movies).adapter as ConcatAdapter
                    previewCount = adapter.adapters.first().itemCount
                }
                SystemClock.sleep(50)
            }
            idle()
            scenario.onActivity { activity ->
                for (id in listOf(R.id.popular, R.id.topRated, R.id.upcoming)) {
                    val row = activity.findViewById<View>(id).findViewById<RecyclerView>(R.id.movies)
                    assertEquals("A newly loaded Home row must start at its first movie", 0,
                        (row.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition())
                }
            }
            scenario.onActivity {
                val section = it.findViewById<View>(R.id.topRated)
                it.findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll).scrollTo(0, section.top)
                section.findViewById<RecyclerView>(R.id.movies).scrollToPosition(10)
            }
            SystemClock.sleep(200)
            idle()
            scenario.onActivity {
                savedTopRatedPosition = (it.findViewById<View>(R.id.topRated).findViewById<RecyclerView>(R.id.movies)
                    .layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                assertTrue(savedTopRatedPosition > 0)
            }
            saveScreenshot("home-row-screen.png")
            scenario.onActivity {
                val section = it.findViewById<View>(R.id.topRated)
                val adapter = section.findViewById<RecyclerView>(R.id.movies).adapter as ConcatAdapter
                assertEquals(10, previewCount)
                assertTrue(adapter.adapters.first() is HomeMovieAdapter)
                assertTrue(adapter.adapters.last() is HomeSeeMoreAdapter)
                assertEquals(11, adapter.itemCount)
                assertTrue(section.findViewById<View>(R.id.seeMore).isShown)
                assertFalse(section.findViewById<View>(R.id.footer).isShown)
                it.navigator.navigate(AppDestination.MovieList(HomeFeed.TOP_RATED))
            }
            idle()
            var listCount = 0
            val listDeadline = SystemClock.elapsedRealtime() + 10_000
            while (listCount != 20 && SystemClock.elapsedRealtime() < listDeadline) {
                scenario.onActivity { listCount = it.findViewById<RecyclerView>(R.id.movies).adapter!!.itemCount }
                SystemClock.sleep(50)
            }
            SystemClock.sleep(200)
            idle()
            saveScreenshot("movie-list-screen.png")
            scenario.onActivity {
                assertEquals(R.id.movie_list, root(it).currentDestination?.id)
                assertEquals("TOP_RATED", root(it).currentBackStackEntry?.arguments?.getString("feed"))
                assertEquals(it.getString(R.string.home_top_rated), it.findViewById<TextView>(R.id.title).text.toString())
                val movies = it.findViewById<RecyclerView>(R.id.movies)
                assertEquals(LinearLayoutManager.VERTICAL, (movies.layoutManager as LinearLayoutManager).orientation)
                assertEquals(20, listCount)
                val entry = root(it).currentBackStackEntry
                it.navigator.navigate(AppDestination.MovieList(HomeFeed.TOP_RATED))
                assertSame(entry, root(it).currentBackStackEntry)
                it.navigator.navigate(AppDestination.Detail(42))
            }
            idle()
            scenario.onActivity {
                assertEquals(R.id.movie_list, root(it).previousBackStackEntry?.destination?.id)
                assertTrue(it.navigator.back())
            }
            idle()
            scenario.onActivity {
                assertEquals(R.id.movie_list, root(it).currentDestination?.id)
                assertEquals("TOP_RATED", root(it).currentBackStackEntry?.arguments?.getString("feed"))
                assertTrue(it.navigator.back())
            }
            idle()
            SystemClock.sleep(200)
            idle()
            scenario.onActivity { activity ->
                assertEquals(R.id.main_screen, root(activity).currentDestination?.id)
                for (id in listOf(R.id.popular, R.id.topRated, R.id.upcoming)) {
                    val row = activity.findViewById<View>(id).findViewById<RecyclerView>(R.id.movies)
                    val expected = if (id == R.id.topRated) savedTopRatedPosition else 0
                    assertEquals("Each Home feed restores its own position", expected,
                        (row.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition())
                }
            }
        }
        Unit
    }
}
