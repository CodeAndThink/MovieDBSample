package com.truongngo.moviedb.domain.model

enum class AppLanguage(val tag: String) {
    ENGLISH("en"),
    VIETNAMESE("vi");

    companion object {
        fun fromTag(tag: String): AppLanguage = entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}
