package dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.thomas_kiljanczyk.openpiano.core.audio.DEFAULT_VELOCITY
import dev.thomas_kiljanczyk.openpiano.core.data.model.KeyboardSettings
import dev.thomas_kiljanczyk.openpiano.core.designsystem.theme.OpenPianoTheme
import dev.thomas_kiljanczyk.openpiano.core.testing.AudioEvent
import dev.thomas_kiljanczyk.openpiano.core.testing.FakeAudioEngine
import dev.thomas_kiljanczyk.openpiano.core.testing.FakeMidiOutputPort
import dev.thomas_kiljanczyk.openpiano.core.testing.MidiOutputEvent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val WHITE_KEY_COUNT = 7
private const val LOWEST_NOTE = 60
private const val WHITE_KEY_ROW = 0.85f
private const val FIRST_WHITE_NOTE = 60

@RunWith(AndroidJUnit4::class)
class KeyboardScreenMidiOutputTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val engine = FakeAudioEngine()
    private val midiOutputPort = FakeMidiOutputPort()

    private fun setContent(midiOutputEnabled: Boolean) {
        val uiState = KeyboardUiState(
            settings = KeyboardSettings.DEFAULT.copy(
                visibleWhiteKeys = WHITE_KEY_COUNT,
                lowestNote = LOWEST_NOTE,
                midiOutputEnabled = midiOutputEnabled,
            ),
            soundReady = true,
        )
        composeTestRule.setContent {
            OpenPianoTheme(dynamicColor = false) {
                KeyboardScreen(
                    uiState = uiState,
                    engine = engine,
                    midiOutputPort = midiOutputPort,
                    onShiftKeys = {},
                    onLowestNoteChange = {},
                    onOpenSettings = {},
                )
            }
        }
    }

    @Test
    fun midiOutputDisabledOnlyDrivesTheSynth() {
        setContent(midiOutputEnabled = false)

        composeTestRule.onNodeWithTag(PIANO_KEYBOARD_TEST_TAG).performTouchInput {
            down(Offset(x = width * 0.5f / WHITE_KEY_COUNT, y = height * WHITE_KEY_ROW))
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf(AudioEvent.NoteOn(FIRST_WHITE_NOTE, DEFAULT_VELOCITY)), engine.events)
        assertEquals(emptyList<MidiOutputEvent>(), midiOutputPort.events)
    }

    @Test
    fun midiOutputEnabledMirrorsTheSynth() {
        setContent(midiOutputEnabled = true)

        composeTestRule.onNodeWithTag(PIANO_KEYBOARD_TEST_TAG).performTouchInput {
            down(Offset(x = width * 0.5f / WHITE_KEY_COUNT, y = height * WHITE_KEY_ROW))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(PIANO_KEYBOARD_TEST_TAG).performTouchInput { up() }
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(
                MidiOutputEvent.NoteOn(FIRST_WHITE_NOTE, DEFAULT_VELOCITY),
                MidiOutputEvent.NoteOff(FIRST_WHITE_NOTE),
            ),
            midiOutputPort.events,
        )
    }
}
