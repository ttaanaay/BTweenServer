package com.btween.app.domain.model

/**
 * The medium a quote originated from. Stored in Room as its [name] via a TypeConverter.
 * Display labels are resolved in the UI layer (see `ui.util.localizedLabel()`) so this
 * domain model has no dependency on Android string resources and stays localization-agnostic.
 */
enum class SourceType {
    MOVIE,
    TV_SERIES,
    BOOK,
    ANIME,
    GAME,
    PODCAST,
    SPEECH,
    OTHER;

    companion object {
        fun fromName(name: String): SourceType =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}
