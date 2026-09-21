package dev.thomas_kiljanczyk.openpiano

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl.navigation.KeyboardNavRoute
import dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl.navigation.keyboardScreen
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.navigation.navigateToSettings
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.navigation.settingsScreen

@Composable
fun OpenPianoNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = KeyboardNavRoute) {
        keyboardScreen(onNavigateToSettings = { navController.navigateToSettings() })
        settingsScreen(onNavigateUp = navController::popBackStack)
    }
}
