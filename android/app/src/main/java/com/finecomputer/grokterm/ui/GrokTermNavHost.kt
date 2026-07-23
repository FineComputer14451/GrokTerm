package com.finecomputer.grokterm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.finecomputer.grokterm.data.OnboardingStore
import com.finecomputer.grokterm.ui.screens.FileBrowserScreen
import com.finecomputer.grokterm.ui.screens.HomeScreen
import com.finecomputer.grokterm.ui.screens.OnboardingScreen
import com.finecomputer.grokterm.ui.screens.SettingsScreen
import com.finecomputer.grokterm.ui.screens.TerminalScreen

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Terminal : Screen("terminal")
    data object Settings : Screen("settings")
    data object Browser : Screen("browser")
}

@Composable
fun GrokTermNavHost() {
    val context = LocalContext.current
    val onboardingStore = remember { OnboardingStore(context) }
    val onboardingComplete by onboardingStore.isCompleteFlow.collectAsState(initial = null)

    if (onboardingComplete == null) return

    val navController = rememberNavController()
    val start = if (onboardingComplete == true) Screen.Home.route else Screen.Onboarding.route

    NavHost(navController = navController, startDestination = start) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenTerminal = { navController.navigate(Screen.Terminal.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenBrowser = { navController.navigate(Screen.Browser.route) }
            )
        }
        composable(Screen.Terminal.route) {
            TerminalScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Browser.route) {
            FileBrowserScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
