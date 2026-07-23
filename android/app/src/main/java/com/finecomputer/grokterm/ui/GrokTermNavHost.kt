package com.finecomputer.grokterm.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.finecomputer.grokterm.ui.screens.HomeScreen
import com.finecomputer.grokterm.ui.screens.SettingsScreen
import com.finecomputer.grokterm.ui.screens.TerminalScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Terminal : Screen("terminal")
    data object Settings : Screen("settings")
}

@Composable
fun GrokTermNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenTerminal = { navController.navigate(Screen.Terminal.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
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
    }
}
