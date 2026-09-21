package com.truongngo.moviedb.presenter.notification

import com.truongngo.moviedb.presenter.navigation.DeepLinkService
import org.junit.Assert.*
import org.junit.Test

class PushNotificationParserTest {
    private val parser = PushNotificationParser(DeepLinkService())

    @Test fun consoleMovieLinkIsPreserved() {
        assertEquals("moviedb://app/detail/550", parser.targetLink("moviedb://app/detail/550"))
    }

    @Test fun missingInvalidAndUnsupportedTargetsFallBackToHome() {
        listOf(null, "", "https://evil.test/detail/1", "moviedb://app/detail/0",
            "moviedb://app/detail/999999999999", "moviedb://app/login",
            "moviedb://app/settings", "moviedb://app/detail/1?x=2").forEach {
            assertEquals("moviedb://app/home", parser.targetLink(it))
        }
    }

    @Test fun ordinaryLauncherIntentHasNoPushTarget() {
        assertNull(parser.consoleTarget(emptyMap()))
    }

    @Test fun consoleMessageWithoutLinkOpensHome() {
        assertEquals("moviedb://app/home", parser.consoleTarget(mapOf("google.message_id" to "message-1")))
    }

    @Test fun consoleExtrasOpenMovieAndWrongTypeFallsBack() {
        assertEquals("moviedb://app/detail/42", parser.consoleTarget(mapOf("deep_link" to "moviedb://app/detail/42")))
        assertEquals("moviedb://app/home", parser.consoleTarget(mapOf("deep_link" to 42)))
    }
}
