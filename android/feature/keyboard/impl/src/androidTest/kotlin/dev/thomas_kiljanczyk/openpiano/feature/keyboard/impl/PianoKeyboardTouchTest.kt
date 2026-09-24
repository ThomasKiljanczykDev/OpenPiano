package dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
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
private val SHIFTED_WHITE_NOTES = listOf(62, 64, 65, 67, 69, 71, 72)
private const val SHIFTED_LOWEST_NOTE = 62
private const val ABOVE_KEYBOARD_Y = -40f

@RunWith(AndroidJUnit4::class)
class PianoKeyboardTouchTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val engine = FakeAudioEngine()

    private var lowestNote by mutableStateOf(LOWEST_NOTE)
    private var keyboardHeight by mutableStateOf(240.dp)
    private var keyboardBounds = Rect.Zero
    private lateinit var rootView: View

    private fun setContent(useAreaHitTest: Boolean = false) {
        composeTestRule.setContent {
            rootView = LocalView.current
            OpenPianoTheme(dynamicColor = false) {
                Column {
                    Spacer(modifier = Modifier.height(80.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 560.dp, height = keyboardHeight)
                            .onGloballyPositioned { keyboardBounds = it.boundsInRoot() }
                            .testTag(KEYBOARD_TAG),
                    ) {
                        PianoKeyboard(
                            lowestNote = lowestNote,
                            whiteKeyCount = WHITE_KEY_COUNT,
                            labelMode = KeyLabelMode.NONE,
                            useAreaHitTest = useAreaHitTest,
                            areaOverlapThreshold = 0.35f,
                            onNoteOn = { engine.noteOn(it, DEFAULT_VELOCITY) },
                            onNoteOff = engine::noteOff,
                        )
                    }
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

    @Test
    fun twoPointersOnTheSameKeyReleaseOnlyWhenTheLastLifts() {
        setContent()

        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput {
            down(0, whiteKeyCenter(1))
            down(1, whiteKeyCenter(1) + Offset(4f, 0f))
            up(0)
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf(AudioEvent.NoteOn(WHITE_NOTES[1], DEFAULT_VELOCITY)), engine.events)

        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput { up(1) }
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(
                AudioEvent.NoteOn(WHITE_NOTES[1], DEFAULT_VELOCITY),
                AudioEvent.NoteOff(WHITE_NOTES[1]),
            ),
            engine.events,
        )
    }

    @Test
    fun leavingCompositionReleasesHeldNotes() {
        var shown by mutableStateOf(true)
        composeTestRule.setContent {
            OpenPianoTheme(dynamicColor = false) {
                Box(modifier = Modifier.size(width = 560.dp, height = 240.dp).testTag(KEYBOARD_TAG)) {
                    if (shown) {
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

        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput { down(whiteKeyCenter(2)) }
        composeTestRule.waitForIdle()
        shown = false
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(
                AudioEvent.NoteOn(WHITE_NOTES[2], DEFAULT_VELOCITY),
                AudioEvent.NoteOff(WHITE_NOTES[2]),
            ),
            engine.events,
        )
    }

    @Test
    fun heldPointerStaysSilentAfterRangeShiftUntilRepressed() {
        setContent()

        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput { down(whiteKeyCenter(0)) }
        composeTestRule.waitForIdle()
        lowestNote = SHIFTED_LOWEST_NOTE
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput {
            moveTo(whiteKeyCenter(1))
            moveTo(whiteKeyCenter(2))
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(
                AudioEvent.NoteOn(WHITE_NOTES[0], DEFAULT_VELOCITY),
                AudioEvent.NoteOff(WHITE_NOTES[0]),
            ),
            engine.events,
        )

        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput { down(whiteKeyCenter(1)) }
        composeTestRule.waitForIdle()

        assertEquals(
            AudioEvent.NoteOn(SHIFTED_WHITE_NOTES[1], DEFAULT_VELOCITY),
            engine.events.last(),
        )
    }

    @Test
    fun pointerOffTheKeysDuringRangeShiftStaysSilentWhenSlidingBack() {
        setContent()

        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput {
            down(whiteKeyCenter(0))
            moveTo(Offset(whiteKeyCenter(0).x, ABOVE_KEYBOARD_Y))
        }
        composeTestRule.waitForIdle()
        lowestNote = SHIFTED_LOWEST_NOTE
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput {
            moveTo(whiteKeyCenter(0))
            moveTo(whiteKeyCenter(1))
        }
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
    fun sizeChangeReleasesAndMutesHeldPointers() {
        setContent()

        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput { down(whiteKeyCenter(0)) }
        composeTestRule.waitForIdle()
        keyboardHeight = 200.dp
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(
                AudioEvent.NoteOn(WHITE_NOTES[0], DEFAULT_VELOCITY),
                AudioEvent.NoteOff(WHITE_NOTES[0]),
            ),
            engine.events,
        )

        composeTestRule.onNodeWithTag(KEYBOARD_TAG).performTouchInput {
            moveTo(whiteKeyCenter(1))
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals(2, engine.events.size)
    }

    // Compose test input reports zero touch size, so a raw MotionEvent is needed to reach the area path.
    @Test
    fun areaHitTestUsesTheTouchSizeOfThePointerOnTheKeyboard() {
        setContent(useAreaHitTest = true)
        composeTestRule.waitForIdle()

        val whiteKeyWidth = keyboardBounds.width / WHITE_KEY_COUNT
        val boundaryBetweenDAndE = keyboardBounds.left + 2 * whiteKeyWidth
        val outside = TouchPoint(x = keyboardBounds.left + 10f, y = keyboardBounds.top / 2f, touchMajor = 1f)
        val onKeys = TouchPoint(
            x = boundaryBetweenDAndE - 0.2f * whiteKeyWidth,
            y = keyboardBounds.top + keyboardBounds.height * WHITE_KEY_ROW,
            touchMajor = 2 * whiteKeyWidth,
            touchMinor = 10f,
        )
        val downTime = SystemClock.uptimeMillis()
        val secondDown = MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        val secondUp = MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)

        dispatch(downTime, MotionEvent.ACTION_DOWN, outside)
        dispatch(downTime, secondDown, outside, onKeys)

        assertEquals(
            setOf(
                AudioEvent.NoteOn(WHITE_NOTES[1], DEFAULT_VELOCITY),
                AudioEvent.NoteOn(WHITE_NOTES[2], DEFAULT_VELOCITY),
            ),
            engine.events.toSet(),
        )

        dispatch(downTime, secondUp, outside, onKeys)
        dispatch(downTime, MotionEvent.ACTION_UP, outside)

        assertEquals(
            setOf(AudioEvent.NoteOff(WHITE_NOTES[1]), AudioEvent.NoteOff(WHITE_NOTES[2])),
            engine.events.drop(2).toSet(),
        )
    }

    private fun dispatch(downTime: Long, action: Int, vararg points: TouchPoint) {
        composeTestRule.runOnIdle {
            val origin = IntArray(2).also(rootView::getLocationOnScreen)
            val properties = Array(points.size) { index ->
                MotionEvent.PointerProperties().apply {
                    id = index
                    toolType = MotionEvent.TOOL_TYPE_FINGER
                }
            }
            // Compose reads raw (screen) coordinates, so build in screen space and shift back to view space.
            val coords = Array(points.size) { index ->
                MotionEvent.PointerCoords().apply {
                    x = points[index].x + origin[0]
                    y = points[index].y + origin[1]
                    pressure = 1f
                    touchMajor = points[index].touchMajor
                    touchMinor = points[index].touchMinor
                }
            }
            val event = MotionEvent.obtain(
                downTime,
                SystemClock.uptimeMillis(),
                action,
                points.size,
                properties,
                coords,
                0,
                0,
                1f,
                1f,
                0,
                0,
                InputDevice.SOURCE_TOUCHSCREEN,
                0,
            )
            event.offsetLocation(-origin[0].toFloat(), -origin[1].toFloat())
            rootView.dispatchTouchEvent(event)
            event.recycle()
        }
        composeTestRule.waitForIdle()
    }

    private data class TouchPoint(
        val x: Float,
        val y: Float,
        val touchMajor: Float,
        val touchMinor: Float = touchMajor,
    )

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
