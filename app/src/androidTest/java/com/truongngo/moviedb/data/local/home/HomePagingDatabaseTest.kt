package com.truongngo.moviedb.data.local.home

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.truongngo.moviedb.data.local.database.HomeCacheDatabase
import com.truongngo.moviedb.data.local.model.HomeCacheKey
import com.truongngo.moviedb.data.network.model.Movie
import com.truongngo.moviedb.data.network.model.MoviePage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class HomePagingDatabaseTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun migratesVersionOneWithoutLosingBanner() = runBlocking {
        val context = instrumentation.targetContext
        val name = "home-migration-${System.nanoTime()}.db"
        // Build the actual exported v1 schema; opening it with Room validates the v1→v2 migration.
        val schema = org.json.JSONObject(instrumentation.context.assets.open(
            "com.truongngo.moviedb.data.local.database.HomeCacheDatabase/1.json"
        ).bufferedReader().use { it.readText() }).getJSONObject("database")
        context.openOrCreateDatabase(name, android.content.Context.MODE_PRIVATE, null).apply {
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) execSQL(setup.getString(index))
            version = 1
            execSQL("INSERT INTO home_pages (feed, language, region, totalPages, totalResults, fetchedAt) VALUES ('NOW_PLAYING', 'en-US', '', 1, 0, 1234)")
            close()
        }
        val db = Room.databaseBuilder(context, HomeCacheDatabase::class.java, name).build()
        try {
            assertEquals(1234L, db.homeDao().page("NOW_PLAYING", "en-US", "")!!.fetchedAt)
            assertNull(db.pagingDao().remoteKey("POPULAR", "en-US", ""))
            assertTrue(db.pagingDao().savePage(HomeCacheKey("POPULAR"), page(1, listOf(7)), null, 5000))
            assertEquals(listOf(7), db.pagingDao().movies("POPULAR", "en-US", "").map { it.movie.movieId })
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun persistsAllPagesAndKeysAndRejectsLateAppendAfterRefresh() = runBlocking {
        val context = instrumentation.targetContext
        val name = "home-paging-${System.nanoTime()}.db"
        fun open() = Room.databaseBuilder(context, HomeCacheDatabase::class.java, name).build()
        var db = open()
        val key = HomeCacheKey("POPULAR")
        val other = HomeCacheKey("TOP_RATED")
        val translated = key.copy(language = "vi-VN", region = "VN")
        try {
            var dao = db.pagingDao()
            dao.savePage(key, page(1, listOf(9, 2, 9)), null, 1000)
            val first = dao.remoteKey(key.feed, key.language, key.region)!!
            dao.savePage(key, page(2, listOf(2, 3)), first, 2000)
            dao.savePage(other, page(1, listOf(4)), null, 1000)
            dao.savePage(translated, page(1, listOf(5)), null, 1000)
            db.close()
            db = open()
            dao = db.pagingDao()
            val cached = dao.remoteKey(key.feed, key.language, key.region)!!
            assertEquals(3, cached.nextPage)
            assertEquals(1000L, cached.refreshedAt)
            val source = dao.pagingSource(key.feed, key.language, key.region)
            val loaded = source.load(PagingSource.LoadParams.Refresh(null, 20, false)) as PagingSource.LoadResult.Page
            assertEquals(listOf(9, 2, 3), loaded.data.map { it.movie.movieId })
            dao.savePage(key, page(1, listOf(77)), cached, 3000)
            assertFalse(dao.savePage(key, page(3, listOf(88)), cached, 4000))
            assertEquals(listOf(77), dao.movies(key.feed, key.language, key.region).map { it.movie.movieId })
            assertEquals(listOf(4), dao.movies(other.feed, other.language, other.region).map { it.movie.movieId })
            assertEquals(listOf(5), dao.movies(translated.feed, translated.language, translated.region).map { it.movie.movieId })
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun failedDatabaseWriteRollsBackRefreshRowsAndRemoteKey() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, HomeCacheDatabase::class.java).build()
        try {
            val dao = db.pagingDao()
            val key = HomeCacheKey("POPULAR")
            dao.savePage(key, page(1, listOf(7)), null, 1000)
            val previous = dao.remoteKey(key.feed, key.language, key.region)
            db.openHelper.writableDatabase.execSQL("""
                CREATE TRIGGER reject_movie BEFORE INSERT ON home_feed_movies
                WHEN NEW.movieId = 99 BEGIN SELECT RAISE(ABORT, 'test write failure'); END
            """.trimIndent())
            try {
                dao.savePage(key, page(1, listOf(99)), previous, 2000)
                fail("Expected database write failure")
            } catch (_: android.database.sqlite.SQLiteException) {
                // The delete and metadata change must be rolled back with the failed insert.
            }
            assertEquals(listOf(7), dao.movies(key.feed, key.language, key.region).map { it.movie.movieId })
            assertEquals(previous, dao.remoteKey(key.feed, key.language, key.region))
        } finally { db.close() }
    }

    @Test fun homePreviewRemainsTenAfterMorePagesAreCached() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(instrumentation.targetContext, HomeCacheDatabase::class.java).build()
        try {
            val dao = db.pagingDao()
            val key = HomeCacheKey("POPULAR")
            dao.savePage(key, page(1, (1..20).toList()), null, 1000)
            val first = dao.remoteKey(key.feed, key.language, key.region)
            dao.savePage(key, page(2, (21..40).toList()), first, 1000)
            val preview = dao.observePreview(key.feed, key.language, key.region).first()
            assertEquals((1..10).toList(), preview.map { it.movie.movieId })
            assertEquals(40, dao.movies(key.feed, key.language, key.region).size)
        } finally { db.close() }
    }

    private fun page(number: Int, ids: List<Int>) = MoviePage(number, ids.map {
        Movie(it, "Movie $it", null, null, null, null, null, 8.0, 10, null, null, null)
    }, 5, 100)
}
