package dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.thomas_kiljanczyk.openpiano.core.audio.DEFAULT_VELOCITY
import dev.thomas_kiljanczyk.openpiano.core.designsystem.theme.OpenPianoTheme
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.testing.AudioEvent
import dev.thomas_kiljanczyk.openpiano.core.testing.FakeAudioEngine
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val KEYBOARD_TAG = "keyboard"
private const val WHITE_KEY_COUNT = 7
private const val LOWEST_NOTE = 60
private const val WHITE_KEY_ROW = 0.85f
private val WHITE_NOTES = listOf(60, 62, 64, 65, 67, 69, 71)

@RunWith(AndroidJUnit4::class)
class PianoKeyboardTouchTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val engine = FakeAudioEngine()

    private fun setContent() {
        composeTestRule.setContent {
            OpenPianoTheme(dynamicColor = false) {
                Box(modifier = Modifier.size(width = 560.dp, height = 240.dp).testTag(KEYBOARD_TAG)) {
                    PianoKeyboard(
                        lowestNote = LOWEST_NOTE,
                        whiteKeyCount = WHITE_KEY_COUNT,
                        labelMode = KeyLabelMode.NONE,
                        useAreaHitTest = false,
                        areaOverlapThreshold = 0.35f,
                        onNoteOn = { engine.noteOn(it, DEFAULT_VELOCITY) },
                        onNoteOff = engine::noteOff,
                    )
                }
            }
        }
    }

    @Test
    fun downThenUpEmitsOneNoteOnAndOneNoteOff() {
        setContent()

        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput { down(whiteKeyCenter(0)) }
        composeTestRule.waitForIdle()

        assertEquals(listOf(AudioEvent.NoteOn(WHITE_NOTES[0], DEFAULT_VELOCITY)), engine.events)

        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput { up() }
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(
                AudioEvent.NoteOn(WHITE_NOTES[0], DEFAULT_VELOCITY),
                AudioEvent.NoteOff(WHITE_NOTES[0]),
            ),
            engine.events,
        )
    }

    @Test
    fun twoPointersEmitTwoDistinctNoteOns() {
        setContent()

        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput {
            down(0, whiteKeyCenter(0))
            down(1, whiteKeyCenter(2))
        }
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(
                AudioEvent.NoteOn(WHITE_NOTES[0], DEFAULT_VELOCITY),
                AudioEvent.NoteOn(WHITE_NOTES[2], DEFAULT_VELOCITY),
            ),
            engine.events,
        )

        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput {
            up(0)
            up(1)
        }
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(
                AudioEvent.NoteOff(WHITE_NOTES[0]),
                AudioEvent.NoteOff(WHITE_NOTES[2]),
            ),
            engine.events.drop(2),
        )
    }

    @Test
    fun draggingAcrossKeysReleasesThenPressesTheNext() {
        setContent()

        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput {
            down(whiteKeyCenter(0))
            moveTo(whiteKeyCenter(1))
        }
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(
                AudioEvent.NoteOn(WHITE_NOTES[0], DEFAULT_VELOCITY),
                AudioEvent.NoteOff(WHITE_NOTES[0]),
                AudioEvent.NoteOn(WHITE_NOTES[1], DEFAULT_VELOCITY),
            ),
            engine.events,
        )
    }

    @Test
    fun holdingEmitsNothingUntilThePointerLifts() {
        setContent()

        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput { down(whiteKeyCenter(3)) }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(HOLD_MILLIS)
        composeTestRule.waitForIdle()

        assertEquals(listOf(AudioEvent.NoteOn(WHITE_NOTES[3], DEFAULT_VELOCITY)), engine.events)

        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput { up() }
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(
                AudioEvent.NoteOn(WHITE_NOTES[3], DEFAULT_VELOCITY),
                AudioEvent.NoteOff(WHITE_NOTES[3]),
            ),
            engine.events,
        )
    }

    private companion object {
        const val HOLD_MILLIS = 2000L
    }
}

// Below the black keys, so every hit resolves to a white key.
private fun TouchInjectionScope.whiteKeyCenter(index: Int): Offset =
    Offset(
        x = width * (index + 0.5f) / WHITE_KEY_COUNT,
        y = height * WHITE_KEY_ROW,
    )
