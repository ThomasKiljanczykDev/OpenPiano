package dev.thomas_kiljanczyk.openpiano.tools.readmescreenshots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import dev.thomas_kiljanczyk.openpiano.core.designsystem.theme.OpenPianoTheme
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.TouchHitTestMode
import dev.thomas_kiljanczyk.openpiano.core.tutorial.TourAnchor
import dev.thomas_kiljanczyk.openpiano.core.tutorial.TourAnchorRegistry
import dev.thomas_kiljanczyk.openpiano.core.tutorial.TourExpandable
import dev.thomas_kiljanczyk.openpiano.core.tutorial.TourExpansion
import dev.thomas_kiljanczyk.openpiano.core.tutorial.TourHost
import dev.thomas_kiljanczyk.openpiano.core.tutorial.TourStep
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LanguageOption
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.ui.SettingsScreen
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.ui.SettingsUiState
import dev.thomas_kiljanczyk.openpiano.tools.screenshotmocks.MOCK_LOWEST_NOTE
import dev.thomas_kiljanczyk.openpiano.tools.screenshotmocks.MOCK_VISIBLE_WHITE_KEYS
import dev.thomas_kiljanczyk.openpiano.tools.screenshotmocks.MockKeyboardScreen

private const val KEYBOARD_WIDTH_DP = 800
private const val KEYBOARD_HEIGHT_DP = 360
private const val SETTINGS_WIDTH_DP = 800
private const val SETTINGS_HEIGHT_DP = 900
private const val STEP_COUNT = 11

// The mock keyboard has no anchors, so the overview bounds follow its fixed layout.
private const val OVERVIEW_LEFT_DP = 100
private const val OVERVIEW_TOP_DP = 56
private const val OVERVIEW_RIGHT_DP = 700
private const val OVERVIEW_BOTTOM_DP = 104

@PreviewTest
@Preview(widthDp = KEYBOARD_WIDTH_DP, heightDp = KEYBOARD_HEIGHT_DP)
@Composable
fun TourStep3OverviewScreenshot() {
    val density = LocalDensity.current
    val registry = remember(density) {
        TourAnchorRegistry().apply {
            with(density) {
                update(
                    TourAnchor.KEYBOARD_OVERVIEW,
                    Rect(
                        OVERVIEW_LEFT_DP.dp.toPx(),
                        OVERVIEW_TOP_DP.dp.toPx(),
                        OVERVIEW_RIGHT_DP.dp.toPx(),
                        OVERVIEW_BOTTOM_DP.dp.toPx(),
                    ),
                )
            }
        }
    }
    OpenPianoTheme(darkTheme = false, dynamicColor = false) {
        TourHost(
            registry = registry,
            step = TourStep(
                id = "keyboard_overview",
                titleRes = R.string.tour_preview_overview_title,
                bodyRes = R.string.tour_preview_overview_body,
                anchors = listOf(TourAnchor.KEYBOARD_OVERVIEW),
            ),
            stepIndex = 2,
            stepCount = STEP_COUNT,
            onNext = {},
            onSkip = {},
        ) {
            MockKeyboardScreen(MOCK_LOWEST_NOTE, MOCK_VISIBLE_WHITE_KEYS, KeyLabelMode.C_ONLY)
        }
    }
}

@PreviewTest
@Preview(widthDp = SETTINGS_WIDTH_DP, heightDp = SETTINGS_HEIGHT_DP)
@Composable
fun TourStep10PrecisionScreenshot() {
    val registry = remember { TourAnchorRegistry() }
    val expansion = remember { TourExpansion { it == TourExpandable.PRECISION_SLIDER } }
    OpenPianoTheme(darkTheme = false, dynamicColor = false) {
        TourHost(
            registry = registry,
            step = TourStep(
                id = "settings_touch_precision",
                titleRes = R.string.tour_preview_precision_title,
                bodyRes = R.string.tour_preview_precision_body,
                anchors = listOf(TourAnchor.SETTINGS_TOUCH_PRECISION),
            ),
            stepIndex = 9,
            stepCount = STEP_COUNT,
            onNext = {},
            onSkip = {},
            expansion = expansion,
        ) {
            SettingsScreen(
                uiState = SettingsUiState(
                    settings = SettingsUiState().settings.copy(touchHitTestMode = TouchHitTestMode.POINT),
                ),
                language = LanguageOption.SYSTEM,
                languageOptions = listOf(LanguageOption.SYSTEM to "System default"),
                onLanguageChange = {},
                onThemeModeChange = {},
                onLabelModeChange = {},
                onVisibleWhiteKeysChange = {},
                onReverbEnabledChange = {},
                onMidiOutputEnabledChange = {},
                onTouchHitTestModeChange = {},
                onAreaOverlapThresholdPercentChange = {},
                onReplayTutorial = {},
                onNavigateUp = {},
            )
        }
    }
}
