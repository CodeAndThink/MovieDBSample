package com.truongngo.moviedb.domain.auth

/** Provider-independent registration failures, translated by each authentication adapter. */
class SignupException(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason { EMAIL_IN_USE, WEAK_PASSWORD, INVALID_EMAIL, NETWORK, TOO_MANY_REQUESTS, UNKNOWN }
}
