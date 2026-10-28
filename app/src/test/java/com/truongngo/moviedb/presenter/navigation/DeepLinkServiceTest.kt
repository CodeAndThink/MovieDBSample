package com.truongngo.moviedb.presenter.navigation

import org.junit.Assert.*
import org.junit.Test

class DeepLinkServiceTest {
    private val resolver = DeepLinkService()

    @Test fun supportedRoutes() {
        listOf(AppDestination.SEARCH, AppDestination.HOME, AppDestination.SETTINGS, AppDestination.LOGIN, AppDestination.SIGNUP).forEach {
            assertEquals(it, resolver.resolve("moviedb://app/${it.encode().lowercase()}"))
        }
    }

    @Test fun detailRoutesAndSavedRoutesRoundTrip() {
        val route = AppDestination.Detail(123)
        assertEquals(route, resolver.resolve("moviedb://app/detail/123"))
        assertEquals(route, AppDestination.decode(route.encode()))
        assertEquals(AppDestination.Detail(Int.MAX_VALUE), resolver.resolve("moviedb://app/detail/2147483647"))
    }

    @Test fun movieListsRoundTripAndRejectUnsupportedFeeds() {
        for (feed in listOf(com.truongngo.moviedb.domain.model.HomeFeed.POPULAR,
            com.truongngo.moviedb.domain.model.HomeFeed.TOP_RATED, com.truongngo.moviedb.domain.model.HomeFeed.UPCOMING)) {
            val route = AppDestination.MovieList(feed)
            assertEquals(route, AppDestination.decode(route.encode()))
            assertEquals(route, resolver.resolve("moviedb://app/movies/${feed.name.lowercase()}"))
            assertTrue(route.requiresAuthentication)
        }
        for (feed in listOf("", "now_playing", "unknown", "POPULAR", "popular/", "popular?x=1")) {
            assertNull(resolver.resolve("moviedb://app/movies/$feed"))
        }
    }

    @Test fun rejectsInvalidMovieIds() {
        listOf("", "0", "-1", "+1", "01", "abc", "2147483648", "1/", "1/2", "%31", "1?x=2", "1#x")
            .forEach { assertNull(it, resolver.resolve("moviedb://app/detail/$it")) }
    }

    @Test fun rejectsUntrustedOrUnsupportedUrls() {
        listOf("", "moviedb:home", "https://app/home", "moviedb://evil/home",
            "moviedb://app.evil/home", "moviedb://user@app/home", "moviedb://app:80/home",
            "moviedb://app/home?next=settings", "moviedb://app/home#settings",
            "moviedb://app/movie/123", "moviedb://app/%68ome", "moviedb://app/../home",
            "moviedb://app//home", "moviedb://app/home/", "moviedb://app/home extra"
        ).forEach { assertNull(it, resolver.resolve(it)) }
    }
}
