package com.btween.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point annotated for Hilt dependency injection. All app-wide
 * singletons (database, repositories, preferences) are provided via Hilt modules
 * defined under the `di` package.
 */
@HiltAndroidApp
class BTweenApplication : Application()
