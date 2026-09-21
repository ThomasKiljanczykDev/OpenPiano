package dev.thomas_kiljanczyk.openpiano.tools.gplayscreenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import dev.thomas_kiljanczyk.openpiano.core.designsystem.theme.OpenPianoTheme
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.Note
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.R
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LanguageOption
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.SupportedLanguages
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.ui.SettingsScreen
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.ui.SettingsUiState
import dev.thomas_kiljanczyk.openpiano.tools.screenshotmocks.MOCK_LOWEST_NOTE
import dev.thomas_kiljanczyk.openpiano.tools.screenshotmocks.MOCK_VISIBLE_WHITE_KEYS
import dev.thomas_kiljanczyk.openpiano.tools.screenshotmocks.MockKeyboardScreen

// 1890x1063 px; Play caps aspect at 2:1.
private const val DEVICE = "spec:width=720dp,height=405dp,dpi=420"
private const val WIDE_VISIBLE_WHITE_KEYS = 17
private val SHIPPED_TAGS = listOf("en", "pl", "es", "pt-BR", "de", "fr")

// Preview names are fastlane locale directories.
@Preview(name = "en-US", locale = "en", device = DEVICE)
@Preview(name = "de-DE", locale = "de", device = DEVICE)
@Preview(name = "es-ES", locale = "es", device = DEVICE)
@Preview(name = "fr-FR", locale = "fr", device = DEVICE)
@Preview(name = "pl-PL", locale = "pl", device = DEVICE)
@Preview(name = "pt-BR", locale = "pt-rBR", device = DEVICE)
annotation class PlayLocalePreviews

@PreviewTest
@PlayLocalePreviews
@Composable
fun Screenshot1KeyboardDark() {
    OpenPianoTheme(darkTheme = true, dynamicColor = false) {
        MockKeyboardScreen(MOCK_LOWEST_NOTE, MOCK_VISIBLE_WHITE_KEYS, KeyLabelMode.C_ONLY)
    }
}

@PreviewTest
@PlayLocalePreviews
@Composable
fun Screenshot2KeyboardLight() {
    OpenPianoTheme(darkTheme = false, dynamicColor = false) {
        MockKeyboardScreen(Note.firstOfOctave(2), WIDE_VISIBLE_WHITE_KEYS, KeyLabelMode.ALL)
    }
}

@PreviewTest
@PlayLocalePreviews
@Composable
fun Screenshot3Settings() {
    OpenPianoTheme(darkTheme = true, dynamicColor = false) {
        SettingsScreen(
            uiState = SettingsUiState(),
            language = LanguageOption.SYSTEM,
            languageOptions = listOf(
                LanguageOption.SYSTEM to stringResource(R.string.settings_language_system_default),
            ) + SHIPPED_TAGS.map { LanguageOption(it) to SupportedLanguages.autonym(it) },
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
