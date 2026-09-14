package com.truongngo.moviedb.presenter.navigation

import java.net.URI
import javax.inject.Inject

/** Strict allowlist; HTTPS can be added once a real domain is verified. */
class DeepLinkService @Inject constructor() {
    private companion object {
        // Add exact host names here after configuring assetlinks.json on your domain.
        // Also add a separate HTTPS intent filter with autoVerify in AndroidManifest.xml.
        // Host names only: no scheme, path, port or wildcard.
        val HTTPS_HOSTS: Set<String> = setOf(
            // "links.your-domain.com",
        )
    }

    fun resolve(url: String): AppDestination? {
        val uri = try { URI(url) } catch (_: Exception) { return null }
        val supportedOrigin =
            (uri.scheme == "moviedb" && uri.rawAuthority == "app") ||
                (uri.scheme == "https" && uri.rawAuthority in HTTPS_HOSTS)

        if (uri.isOpaque || !supportedOrigin || uri.rawQuery != null || uri.rawFragment != null) return null
        return when (uri.rawPath) {
            "/search" -> AppDestination.SEARCH
            "/home" -> AppDestination.HOME
            "/settings" -> AppDestination.SETTINGS
            "/login" -> AppDestination.LOGIN
            "/signup" -> AppDestination.SIGNUP
            else -> {
                val id = Regex("/detail/([1-9][0-9]*)").matchEntire(uri.rawPath.orEmpty())
                    ?.groupValues?.get(1)?.toIntOrNull()
                id?.let(AppDestination::Detail)
            }
        }
    }
}
