package dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import dev.thomas_kiljanczyk.openpiano.core.audio.AudioEngine
import dev.thomas_kiljanczyk.openpiano.core.data.model.KeyboardSettings
import dev.thomas_kiljanczyk.openpiano.core.designsystem.theme.OpenPianoTheme
import dev.thomas_kiljanczyk.openpiano.core.midi.MidiOutputPort
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.Piano
import dev.thomas_kiljanczyk.openpiano.core.model.TouchHitTestMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val SCREEN_WIDTH_DP = 800
private const val SCREEN_HEIGHT_DP = 360
private const val OVERVIEW_WIDTH_DP = 600
private const val OVERVIEW_HEIGHT_DP = 44
private const val KEYBOARD_WIDTH_DP = 800
private const val KEYBOARD_HEIGHT_DP = 240

private const val NARROW_WHITE_KEYS = 7
private const val WIDE_WHITE_KEYS = 21

private object NoOpAudioEngine : AudioEngine {
    override val isReady: StateFlow<Boolean> = MutableStateFlow(true)

    override fun start() = Unit

    override fun stop() = Unit

    override fun noteOn(note: Int, velocity: Int) = Unit

    override fun noteOff(note: Int) = Unit

    override fun setReverbEnabled(enabled: Boolean) = Unit

    override fun release() = Unit
}

private object NoOpMidiOutputPort : MidiOutputPort {
    override val isConnected: StateFlow<Boolean> = MutableStateFlow(false)

    override fun noteOn(note: Int, velocity: Int) = Unit

    override fun noteOff(note: Int) = Unit
}

private fun state(
    visibleWhiteKeys: Int = KeyboardSettings.DEFAULT.visibleWhiteKeys,
    lowestNote: Int = KeyboardSettings.DEFAULT.lowestNote,
    labelMode: KeyLabelMode = KeyboardSettings.DEFAULT.labelMode,
    reverbEnabled: Boolean = KeyboardSettings.DEFAULT.reverbEnabled,
    midiOutputEnabled: Boolean = KeyboardSettings.DEFAULT.midiOutputEnabled,
    touchHitTestMode: TouchHitTestMode = KeyboardSettings.DEFAULT.touchHitTestMode,
    areaOverlapThresholdPercent: Int = KeyboardSettings.DEFAULT.areaOverlapThresholdPercent,
    soundReady: Boolean = true,
) = KeyboardUiState(
    settings = KeyboardSettings(
        visibleWhiteKeys = visibleWhiteKeys,
        lowestNote = KeyboardSettings.clampLowestNote(lowestNote, visibleWhiteKeys),
        labelMode = labelMode,
        reverbEnabled = reverbEnabled,
        midiOutputEnabled = midiOutputEnabled,
        touchHitTestMode = touchHitTestMode,
        areaOverlapThresholdPercent = areaOverlapThresholdPercent,
    ),
    soundReady = soundReady,
)

private class KeyboardUiStateProvider : PreviewParameterProvider<KeyboardUiState> {
    override val values = sequenceOf(
        KeyboardUiState(),
        state(),
        state(soundReady = false),
        state(labelMode = KeyLabelMode.NONE),
        state(labelMode = KeyLabelMode.C_ONLY),
        state(labelMode = KeyLabelMode.ALL),
        state(visibleWhiteKeys = WIDE_WHITE_KEYS),
        state(visibleWhiteKeys = NARROW_WHITE_KEYS),
        state(lowestNote = Piano.LOWEST_MIDI),
        state(lowestNote = Piano.HIGHEST_MIDI),
    )
}

@Composable
private fun PreviewKeyboardScreen(uiState: KeyboardUiState) {
    KeyboardScreen(
        uiState = uiState,
        engine = NoOpAudioEngine,
        midiOutputPort = NoOpMidiOutputPort,
        onShiftKeys = {},
        onLowestNoteChange = {},
        onOpenSettings = {},
    )
}

@Preview(widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@Composable
private fun KeyboardScreenStatesPreview(
    @PreviewParameter(KeyboardUiStateProvider::class) uiState: KeyboardUiState,
) {
    OpenPianoTheme(dynamicColor = false) {
        PreviewKeyboardScreen(uiState)
    }
}

@Preview(widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@Composable
private fun KeyboardScreenLightPreview() {
    OpenPianoTheme(darkTheme = false, dynamicColor = false) {
        PreviewKeyboardScreen(state())
    }
}

@Preview(widthDp = SCREEN_WIDTH_DP, heightDp = SCREEN_HEIGHT_DP)
@Composable
private fun KeyboardScreenDarkPreview() {
    OpenPianoTheme(darkTheme = true, dynamicColor = false) {
        PreviewKeyboardScreen(state())
    }
}

@Preview(widthDp = OVERVIEW_WIDTH_DP, heightDp = OVERVIEW_HEIGHT_DP)
@Composable
private fun KeyboardOverviewLowPreview() {
    val settings = state(lowestNote = Piano.LOWEST_MIDI).settings
    OpenPianoTheme(dynamicColor = false) {
        KeyboardOverview(
            lowestNote = settings.lowestNote,
            visibleWhiteKeys = settings.visibleWhiteKeys,
            onLowestNoteChange = {},
        )
    }
}

@Preview(widthDp = OVERVIEW_WIDTH_DP, heightDp = OVERVIEW_HEIGHT_DP)
@Composable
private fun KeyboardOverviewMiddlePreview() {
    val settings = state().settings
    OpenPianoTheme(dynamicColor = false) {
        KeyboardOverview(
            lowestNote = settings.lowestNote,
            visibleWhiteKeys = settings.visibleWhiteKeys,
            onLowestNoteChange = {},
        )
    }
}

@Preview(widthDp = OVERVIEW_WIDTH_DP, heightDp = OVERVIEW_HEIGHT_DP)
@Composable
private fun KeyboardOverviewHighPreview() {
    val settings = state(lowestNote = Piano.HIGHEST_MIDI).settings
    OpenPianoTheme(dynamicColor = false) {
        KeyboardOverview(
            lowestNote = settings.lowestNote,
            visibleWhiteKeys = settings.visibleWhiteKeys,
            onLowestNoteChange = {},
        )
    }
}

@Composable
private fun PreviewPianoKeyboard(labelMode: KeyLabelMode, visibleWhiteKeys: Int) {
    OpenPianoTheme(dynamicColor = false) {
        PianoKeyboard(
            lowestNote = KeyboardSettings.clampLowestNote(
                KeyboardSettings.DEFAULT.lowestNote,
                visibleWhiteKeys,
            ),
            whiteKeyCount = visibleWhiteKeys,
            labelMode = labelMode,
            useAreaHitTest = false,
            areaOverlapThreshold = KeyboardSettings.DEFAULT.areaOverlapThresholdPercent / 100f,
            onNoteOn = {},
            onNoteOff = {},
        )
    }
}

@Preview(widthDp = KEYBOARD_WIDTH_DP, heightDp = KEYBOARD_HEIGHT_DP)
@Composable
private fun PianoKeyboardNoLabelsPreview() {
    PreviewPianoKeyboard(KeyLabelMode.NONE, KeyboardSettings.DEFAULT.visibleWhiteKeys)
}

@Preview(widthDp = KEYBOARD_WIDTH_DP, heightDp = KEYBOARD_HEIGHT_DP)
@Composable
private fun PianoKeyboardCOnlyLabelsPreview() {
    PreviewPianoKeyboard(KeyLabelMode.C_ONLY, KeyboardSettings.DEFAULT.visibleWhiteKeys)
}

@Preview(widthDp = KEYBOARD_WIDTH_DP, heightDp = KEYBOARD_HEIGHT_DP)
@Composable
private fun PianoKeyboardAllLabelsPreview() {
    PreviewPianoKeyboard(KeyLabelMode.ALL, KeyboardSettings.DEFAULT.visibleWhiteKeys)
}

@Preview(widthDp = KEYBOARD_WIDTH_DP, heightDp = KEYBOARD_HEIGHT_DP)
@Composable
private fun PianoKeyboardNarrowPreview() {
    PreviewPianoKeyboard(KeyLabelMode.ALL, NARROW_WHITE_KEYS)
}

@Preview(widthDp = KEYBOARD_WIDTH_DP, heightDp = KEYBOARD_HEIGHT_DP)
@Composable
private fun PianoKeyboardWidePreview() {
    PreviewPianoKeyboard(KeyLabelMode.C_ONLY, WIDE_WHITE_KEYS)
}
