package com.btween.app.domain.repository

import com.btween.app.domain.model.SortOrder
import com.btween.app.domain.model.ThemeMode
import com.btween.app.domain.model.UserSettings
import com.btween.app.domain.model.ViewMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val userSettings: Flow<UserSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setUseDynamicColor(enabled: Boolean)

    suspend fun setLibraryViewMode(mode: ViewMode)

    suspend fun setLibrarySortOrder(order: SortOrder)

    suspend fun setOnboardingComplete(completed: Boolean)
}
