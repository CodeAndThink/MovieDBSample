package com.truongngo.moviedb.presenter.notification

import com.truongngo.moviedb.presenter.navigation.AppDestination
import com.truongngo.moviedb.presenter.navigation.DeepLinkService
import javax.inject.Inject

class PushNotificationParser @Inject constructor(private val links: DeepLinkService) {
    fun targetLink(raw: String?): String = when (raw?.let(links::resolve)) {
        is AppDestination.Detail, AppDestination.HOME -> raw
        else -> HOME_LINK
    }

    fun consoleTarget(extras: Map<String, Any?>): String? {
        if (!extras.containsKey("google.message_id") && !extras.containsKey("deep_link")) return null
        return targetLink(extras["deep_link"] as? String)
    }

    companion object {
        const val HOME_LINK = "moviedb://app/home"
    }
}
