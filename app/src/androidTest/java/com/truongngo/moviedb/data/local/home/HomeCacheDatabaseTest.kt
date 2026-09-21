package com.truongngo.moviedb.data.local.home

import com.truongngo.moviedb.data.local.database.HomeCacheDatabase
import com.truongngo.moviedb.data.local.model.HomeCacheKey
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.truongngo.moviedb.data.network.model.Movie
import com.truongngo.moviedb.data.network.model.MoviePage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class HomeCacheDatabaseTest {
    @Test fun persistsOrderedPagesAcrossReopenAndReplacesWithoutAffectingOtherKeys() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "home-cache-test-${System.nanoTime()}.db"
        fun open() = Room.databaseBuilder(context, HomeCacheDatabase::class.java, name).build()
        var db = open()
        val key = HomeCacheKey("POPULAR")
        val vietnamese = key.copy(language = "vi-VN")
        val regional = key.copy(region = "VN")
        val other = HomeCacheKey("TOP_RATED")
        try {
            var cache = RoomHomeCache(db.homeDao())
            cache.write(key, page(listOf(9, 2, 9)))
            cache.write(vietnamese, page(listOf(3)))
            cache.write(regional, page(listOf(4)))
            cache.write(other, page(listOf(5)))
            db.close()
            db = open()
            cache = RoomHomeCache(db.homeDao())
            assertEquals(listOf(9, 2), cache.read(key)!!.page.results.map { it.id })
            assertEquals(page(listOf(9, 2)), cache.read(key)!!.page)
            assertEquals(7, cache.read(key)!!.page.totalPages)
            assertTrue(cache.read(key)!!.isFresh(System.currentTimeMillis()))
            cache.write(key, page(emptyList()))
            assertTrue(cache.read(key)!!.page.results.isEmpty())
            assertEquals(listOf(3), cache.read(vietnamese)!!.page.results.map { it.id })
            assertEquals(listOf(4), cache.read(regional)!!.page.results.map { it.id })
            assertEquals(listOf(5), cache.read(other)!!.page.results.map { it.id })
            assertNull(cache.read(HomeCacheKey("UPCOMING")))
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    private fun page(ids: List<Int>) = MoviePage(1, ids.map {
        Movie(it, "Movie $it", "Overview", null, "/poster.jpg", null, "2026-01-01", 8.0, 10, listOf(1, 2), null, null)
    }, 7, 140)
}
