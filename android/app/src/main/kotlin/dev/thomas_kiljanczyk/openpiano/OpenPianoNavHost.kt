package dev.thomas_kiljanczyk.openpiano

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dev.thomas_kiljanczyk.openpiano.core.tutorial.TourAnchor
import dev.thomas_kiljanczyk.openpiano.core.tutorial.TourExpandable
import dev.thomas_kiljanczyk.openpiano.core.tutorial.TourExpansion
import dev.thomas_kiljanczyk.openpiano.core.tutorial.TourHost
import dev.thomas_kiljanczyk.openpiano.core.tutorial.rememberTourAnchorRegistry
import dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl.navigation.KeyboardNavRoute
import dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl.navigation.keyboardScreen
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.navigation.navigateToSettings
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.navigation.settingsScreen
import dev.thomas_kiljanczyk.openpiano.tutorial.TutorialPhase
import dev.thomas_kiljanczyk.openpiano.tutorial.TutorialViewModel
import dev.thomas_kiljanczyk.openpiano.tutorial.rememberTourSteps

@Composable
fun OpenPianoNavHost(tutorialViewModel: TutorialViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val steps = rememberTourSteps(navController)
    val registry = rememberTourAnchorRegistry()

    val phase = tutorialViewModel.phase
    val stepIndex = (phase as? TutorialPhase.Tour)?.stepIndex ?: 0
    val currentStep = steps.getOrNull(stepIndex).takeIf { phase is TutorialPhase.Tour }

    val endTour: () -> Unit = {
        tutorialViewModel.finish()
        navController.popBackStack(KeyboardNavRoute, inclusive = false)
    }

    // After composition, so destination anchors lay out before the overlay looks for them.
    LaunchedEffect(currentStep) { currentStep?.onEnter?.invoke() }

    val forcesPrecisionSlider = currentStep?.anchors?.contains(TourAnchor.SETTINGS_TOUCH_PRECISION) == true
    val expansion = remember(forcesPrecisionSlider) {
        TourExpansion { it == TourExpandable.PRECISION_SLIDER && forcesPrecisionSlider }
    }

    TourHost(
        registry = registry,
        step = currentStep,
        stepIndex = stepIndex,
        stepCount = steps.size,
        onNext = {
            if (stepIndex >= steps.lastIndex) {
                endTour()
            } else {
                tutorialViewModel.next(steps.size)
            }
        },
        onSkip = endTour,
        expansion = expansion,
    ) {
        NavHost(
            navController = navController,
            startDestination = KeyboardNavRoute,
        ) {
            keyboardScreen(onNavigateToSettings = { navController.navigateToSettings() })
            settingsScreen(onNavigateUp = navController::popBackStack)
        }
    }
}
