package com.btween.app.domain.model

/**
 * Display labels are resolved in the UI layer (see `ui.util.localizedLabel()`) so this
 * domain model has no dependency on Android string resources.
 */
enum class SortOrder {
    NEWEST,
    OLDEST,
    ALPHABETICAL,
    FAVORITE
}
