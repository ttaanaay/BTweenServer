package com.btween.app.domain.model

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = false,
    val libraryViewMode: ViewMode = ViewMode.LIST,
    val librarySortOrder: SortOrder = SortOrder.NEWEST,
    val hasCompletedOnboarding: Boolean = false
)
