package dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl.KeyboardRoute
import kotlinx.serialization.Serializable

@Serializable
data object KeyboardNavRoute

fun NavGraphBuilder.keyboardScreen(onNavigateToSettings: () -> Unit) {
    composable<KeyboardNavRoute> {
        KeyboardRoute(onNavigateToSettings = onNavigateToSettings)
    }
}
