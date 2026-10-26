package com.truongngo.moviedb.data.paging

import androidx.paging.*
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.truongngo.moviedb.data.local.database.HomeCacheDatabase
import com.truongngo.moviedb.data.local.model.HomeCacheKey
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalPagingApi::class)
class HomePagingFlowTest {
    @Test fun duplicateOnlyMiddlePageDoesNotPreventLoadingLaterMovies() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,
            HomeCacheDatabase::class.java).build()
        try {
            val dao = db.pagingDao()
            val api = PagingTestApi().apply {
                popular = { moviePage(it, when (it) { 1 -> listOf(1, 2); 2 -> listOf(2, 1); else -> listOf(3) }) }
            }
            val key = HomeCacheKey("POPULAR")
            val pager = Pager(PagingConfig(pageSize = 2, initialLoadSize = 2, prefetchDistance = 1, enablePlaceholders = false),
                remoteMediator = HomeRemoteMediator(api, dao, key),
                pagingSourceFactory = { dao.pagingSource(key.feed, key.language, key.region) })
            val snapshot = withTimeout(15_000) { pager.flow.asSnapshot { scrollTo(2) } }
            assertEquals(listOf(1, 2, 3), snapshot.map { it.movie.movieId })
            assertEquals(listOf(1, 2, 3), api.popularCalls)
        } finally { db.close() }
    }
    @Test fun reopenedPagerReadsEveryCachedPageWithoutNetwork() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "home-offline-${System.nanoTime()}.db"
        fun open() = Room.databaseBuilder(context, HomeCacheDatabase::class.java, name).build()
        var db = open()
        val key = HomeCacheKey("POPULAR")
        try {
            val now = System.currentTimeMillis()
            db.pagingDao().savePage(key, moviePage(1, listOf(9, 2), total = 2), null, now)
            val first = db.pagingDao().remoteKey(key.feed, key.language, key.region)
            db.pagingDao().savePage(key, moviePage(2, listOf(3, 4), total = 2), first, now)
            db.close()
            db = open()
            val dao = db.pagingDao()
            val offlineApi = PagingTestApi().apply { popular = { throw java.io.IOException("offline") } }
            val pager = Pager(PagingConfig(pageSize = 2, initialLoadSize = 2, prefetchDistance = 1, enablePlaceholders = false),
                remoteMediator = HomeRemoteMediator(offlineApi, dao, key),
                pagingSourceFactory = { dao.pagingSource(key.feed, key.language, key.region) })
            val snapshot = withTimeout(15_000) { pager.flow.asSnapshot { scrollTo(3) } }
            assertEquals(listOf(9, 2, 3, 4), snapshot.map { it.movie.movieId })
            assertTrue(offlineApi.popularCalls.isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

}
