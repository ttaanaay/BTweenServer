package com.btween.app.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.btween.app.domain.model.AppLanguage

/**
 * Wraps AndroidX's per-app language API (`AppCompatDelegate.setApplicationLocales`).
 *
 * This works even though the app uses plain `ComponentActivity` rather than
 * `AppCompatActivity`: since appcompat 1.6.0, simply having the appcompat dependency
 * present is enough - it registers a ContentProvider at process start that reads the
 * persisted locale choice and applies + auto-recreates activities for you, on every
 * API level back to 24. On API 33+ it additionally delegates to the platform
 * `LocaleManager` so the choice also shows up in the system's own per-app language
 * settings screen (enabled via `android:localeConfig` in the manifest).
 */
object LocaleManager {

    fun applyLanguage(language: AppLanguage) {
        val localeList = when (language) {
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
            AppLanguage.THAI -> LocaleListCompat.forLanguageTags("th")
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun currentLanguage(): AppLanguage {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return when {
            tags.startsWith("th") -> AppLanguage.THAI
            tags.startsWith("en") -> AppLanguage.ENGLISH
            else -> AppLanguage.SYSTEM
        }
    }
}
