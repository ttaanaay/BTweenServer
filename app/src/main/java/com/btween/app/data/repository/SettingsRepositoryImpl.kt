package com.btween.app.data.repository

import com.btween.app.data.preferences.UserPreferencesManager
import com.btween.app.domain.model.SortOrder
import com.btween.app.domain.model.ThemeMode
import com.btween.app.domain.model.UserSettings
import com.btween.app.domain.model.ViewMode
import com.btween.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesManager: UserPreferencesManager
) : SettingsRepository {

    override val userSettings: Flow<UserSettings> = preferencesManager.userSettings

    override suspend fun setThemeMode(mode: ThemeMode) = preferencesManager.setThemeMode(mode)

    override suspend fun setUseDynamicColor(enabled: Boolean) =
        preferencesManager.setUseDynamicColor(enabled)

    override suspend fun setLibraryViewMode(mode: ViewMode) =
        preferencesManager.setLibraryViewMode(mode)

    override suspend fun setLibrarySortOrder(order: SortOrder) =
        preferencesManager.setLibrarySortOrder(order)

    override suspend fun setOnboardingComplete(completed: Boolean) =
        preferencesManager.setOnboardingComplete(completed)
}
