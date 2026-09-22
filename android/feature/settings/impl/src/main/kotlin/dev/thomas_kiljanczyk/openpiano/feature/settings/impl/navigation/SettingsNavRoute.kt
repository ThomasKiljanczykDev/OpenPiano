package dev.thomas_kiljanczyk.openpiano.feature.settings.impl.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.ui.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object SettingsNavRoute

fun NavController.navigateToSettings(navOptions: NavOptions? = null) =
    navigate(route = SettingsNavRoute, navOptions)

fun NavGraphBuilder.settingsScreen(onNavigateUp: () -> Unit) {
    composable<SettingsNavRoute> {
        SettingsRoute(onNavigateUp = onNavigateUp)
    }
}
