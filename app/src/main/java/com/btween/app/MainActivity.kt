package com.btween.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.btween.app.ui.MainViewModel
import com.btween.app.ui.auth.LoginScreen
import com.btween.app.ui.auth.RegisterScreen
import com.btween.app.ui.navigation.BTweenBottomNavBar
import com.btween.app.ui.navigation.BTweenNavHost
import com.btween.app.ui.navigation.Destination
import com.btween.app.ui.navigation.bottomNavItems
import com.btween.app.ui.resolveIsDark
import com.btween.app.ui.theme.BTweenTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host for the entire app. Below the Hilt-provided theme, the app is gated
 * on [MainViewModel.isLoggedIn]: signed-out users see the Login/Register flow ([AuthGate]);
 * once signed in, the main app ([BTweenNavHost] + bottom nav) takes over. The bottom
 * navigation bar itself is shown only on the four top-level destinations (Home, Library,
 * Favorites, Settings) and hidden on Detail/Add-Edit/Search so those feel like focused,
 * full-screen flows.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val userSettings by mainViewModel.userSettings.collectAsStateWithLifecycle()
            val isLoggedIn by mainViewModel.isLoggedIn.collectAsStateWithLifecycle()
            val systemInDarkTheme = isSystemInDarkTheme()

            BTweenTheme(
                darkTheme = userSettings.themeMode.resolveIsDark(systemInDarkTheme),
                dynamicColor = userSettings.useDynamicColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isLoggedIn) {
                        MainAppContent()
                    } else {
                        AuthGate()
                    }
                }
            }
        }
    }
}

@Composable
private fun MainAppContent() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentRoute?.hierarchy?.any { it.route == item.destination.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) BTweenBottomNavBar(navController)
        }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            BTweenNavHost(navController = navController)
        }
    }
}

@Composable
private fun AuthGate() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Destination.Login.route) {
        composable(Destination.Login.route) {
            LoginScreen(
                onLoginSuccess = { /* isLoggedIn flips automatically via TokenManager's StateFlow */ },
                onNavigateToRegister = { navController.navigate(Destination.Register.route) }
            )
        }
        composable(Destination.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { /* isLoggedIn flips automatically via TokenManager's StateFlow */ },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
    }
}
