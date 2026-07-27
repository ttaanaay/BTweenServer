package com.btween.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.btween.app.domain.model.SortOrder
import com.btween.app.domain.model.ThemeMode
import com.btween.app.domain.model.UserSettings
import com.btween.app.domain.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin, typed wrapper around a Preferences DataStore. Exposes a single observable
 * [UserSettings] snapshot plus individual setters, so ViewModels never touch raw
 * Preferences keys directly.
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val LIBRARY_VIEW_MODE = stringPreferencesKey("library_view_mode")
        val LIBRARY_SORT_ORDER = stringPreferencesKey("library_sort_order")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("has_completed_onboarding")
    }

    val userSettings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            libraryViewMode = prefs[Keys.LIBRARY_VIEW_MODE]?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() }
                ?: ViewMode.LIST,
            librarySortOrder = prefs[Keys.LIBRARY_SORT_ORDER]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
                ?: SortOrder.NEWEST,
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_COMPLETE] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setLibraryViewMode(mode: ViewMode) {
        dataStore.edit { it[Keys.LIBRARY_VIEW_MODE] = mode.name }
    }

    suspend fun setLibrarySortOrder(order: SortOrder) {
        dataStore.edit { it[Keys.LIBRARY_SORT_ORDER] = order.name }
    }

    suspend fun setOnboardingComplete(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = completed }
    }
}
