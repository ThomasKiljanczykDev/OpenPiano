package dev.thomas_kiljanczyk.openpiano.tools.readmescreenshots

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import dev.thomas_kiljanczyk.openpiano.core.designsystem.theme.OpenPianoTheme
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LanguageOption
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.ui.SettingsScreen
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.ui.SettingsUiState

private const val SCREEN_WIDTH_DP = 800
private const val SCREEN_HEIGHT_DP = 480

@PreviewTest
@Preview(widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@Composable
fun SettingsScreenDarkScreenshot() {
    OpenPianoTheme(darkTheme = true, dynamicColor = false) {
            SettingsScreen(
                uiState = SettingsUiState(),
                language = LanguageOption.SYSTEM,
                languageOptions = listOf(
                    LanguageOption.SYSTEM to "System default",
                    LanguageOption("en") to "English",
                ),
                onLanguageChange = {},
                onLabelModeChange = {},
                onVisibleWhiteKeysChange = {},
                onReverbEnabledChange = {},
                onMidiOutputEnabledChange = {},
                onTouchHitTestModeChange = {},
                onAreaOverlapThresholdPercentChange = {},
                onNavigateUp = {},
            )
    }
}
