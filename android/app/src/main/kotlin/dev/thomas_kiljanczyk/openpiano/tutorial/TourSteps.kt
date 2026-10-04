package dev.thomas_kiljanczyk.openpiano.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import dev.thomas_kiljanczyk.openpiano.R
import dev.thomas_kiljanczyk.openpiano.core.tutorial.TourAnchor
import dev.thomas_kiljanczyk.openpiano.core.tutorial.TourCardPosition
import dev.thomas_kiljanczyk.openpiano.core.tutorial.TourStep
import dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl.navigation.KeyboardNavRoute
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.navigation.SettingsNavRoute
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.navigation.navigateToSettings

/** Stable across recompositions: the tour host keys its effects on step equality. */
@Composable
fun rememberTourSteps(navController: NavController): List<TourStep> = remember(navController) {
    val toKeyboard: () -> Unit = { navController.popBackStack(KeyboardNavRoute, inclusive = false) }

    keyboardSteps(toKeyboard) + settingsSteps(navController)
}

private fun keyboardSteps(toKeyboard: () -> Unit) =
    listOf(
        TourStep(
            id = "keyboard_welcome",
            titleRes = R.string.tour_keyboard_welcome_title,
            bodyRes = R.string.tour_keyboard_welcome_body,
            onEnter = toKeyboard,
        ),
        TourStep(
            id = "keyboard_keys",
            titleRes = R.string.tour_keyboard_keys_title,
            bodyRes = R.string.tour_keyboard_keys_body,
            anchors = listOf(TourAnchor.KEYBOARD_KEYS),
            cardPosition = TourCardPosition.TOP,
            onEnter = toKeyboard,
        ),
        TourStep(
            id = "keyboard_overview",
            titleRes = R.string.tour_keyboard_overview_title,
            bodyRes = R.string.tour_keyboard_overview_body,
            anchors = listOf(TourAnchor.KEYBOARD_OVERVIEW),
            onEnter = toKeyboard,
        ),
        TourStep(
            id = "keyboard_shift",
            titleRes = R.string.tour_keyboard_shift_title,
            bodyRes = R.string.tour_keyboard_shift_body,
            anchors = listOf(TourAnchor.KEYBOARD_SHIFT_DOWN, TourAnchor.KEYBOARD_SHIFT_UP),
            onEnter = toKeyboard,
        ),
        TourStep(
            id = "keyboard_lowest_note",
            titleRes = R.string.tour_keyboard_lowest_note_title,
            bodyRes = R.string.tour_keyboard_lowest_note_body,
            anchors = listOf(TourAnchor.KEYBOARD_LOWEST_NOTE),
            onEnter = toKeyboard,
        ),
        TourStep(
            id = "keyboard_settings",
            titleRes = R.string.tour_keyboard_settings_title,
            bodyRes = R.string.tour_keyboard_settings_body,
            anchors = listOf(TourAnchor.KEYBOARD_SETTINGS),
            onEnter = toKeyboard,
        ),
    )

private fun settingsSteps(navController: NavController) =
    listOf(
        TourStep(
            id = "settings_intro",
            titleRes = R.string.tour_settings_intro_title,
            bodyRes = R.string.tour_settings_intro_body,
            onEnter = {
                if (navController.currentDestination?.hasRoute<SettingsNavRoute>() != true) {
                    navController.navigateToSettings()
                }
            },
        ),
        TourStep(
            id = "settings_visible_keys",
            titleRes = R.string.tour_settings_visible_keys_title,
            bodyRes = R.string.tour_settings_visible_keys_body,
            anchors = listOf(TourAnchor.SETTINGS_VISIBLE_KEYS),
        ),
        TourStep(
            id = "settings_touch_mode",
            titleRes = R.string.tour_settings_touch_mode_title,
            bodyRes = R.string.tour_settings_touch_mode_body,
            anchors = listOf(TourAnchor.SETTINGS_TOUCH_MODE),
        ),
        TourStep(
            id = "settings_touch_precision",
            titleRes = R.string.tour_settings_touch_precision_title,
            bodyRes = R.string.tour_settings_touch_precision_body,
            anchors = listOf(TourAnchor.SETTINGS_TOUCH_PRECISION),
        ),
        TourStep(
            id = "settings_replay",
            titleRes = R.string.tour_settings_replay_title,
            bodyRes = R.string.tour_settings_replay_body,
            anchors = listOf(TourAnchor.SETTINGS_REPLAY_TUTORIAL),
        ),
    )
