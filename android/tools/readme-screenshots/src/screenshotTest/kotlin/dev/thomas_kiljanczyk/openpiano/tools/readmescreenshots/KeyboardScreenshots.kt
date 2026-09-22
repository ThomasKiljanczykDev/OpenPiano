package dev.thomas_kiljanczyk.openpiano.tools.readmescreenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import dev.thomas_kiljanczyk.openpiano.core.designsystem.theme.OpenPianoTheme
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.tools.screenshotmocks.MOCK_LOWEST_NOTE
import dev.thomas_kiljanczyk.openpiano.tools.screenshotmocks.MOCK_VISIBLE_WHITE_KEYS
import dev.thomas_kiljanczyk.openpiano.tools.screenshotmocks.MockKeyboardScreen

private const val SCREEN_WIDTH_DP = 800
private const val SCREEN_HEIGHT_DP = 360

@PreviewTest
@Preview(widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@Composable
fun KeyboardScreenLightScreenshot() {
    OpenPianoTheme(darkTheme = false, dynamicColor = false) {
        MockKeyboardScreen(MOCK_LOWEST_NOTE, MOCK_VISIBLE_WHITE_KEYS, KeyLabelMode.C_ONLY)
    }
}

@PreviewTest
@Preview(widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@Composable
fun KeyboardScreenDarkScreenshot() {
    OpenPianoTheme(darkTheme = true, dynamicColor = false) {
        MockKeyboardScreen(MOCK_LOWEST_NOTE, MOCK_VISIBLE_WHITE_KEYS, KeyLabelMode.C_ONLY)
    }
}
