package dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import dev.thomas_kiljanczyk.openpiano.core.designsystem.theme.LocalKeyColors
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.KeyRect
import dev.thomas_kiljanczyk.openpiano.core.model.KeyboardLayout
import dev.thomas_kiljanczyk.openpiano.core.model.Note

private const val LABEL_TEXT_SIZE_SP = 11
private const val LABEL_BOTTOM_PADDING = 12f

/**
 * Draws every key in a single Canvas and owns one container-level pointer handler. A composable per
 * key would recompose hundreds of nodes per touch and fight over pointer ownership.
 */
@Composable
fun PianoKeyboard(
    lowestNote: Int,
    whiteKeyCount: Int,
    labelMode: KeyLabelMode,
    useAreaHitTest: Boolean,
    areaOverlapThreshold: Float,
    onNoteOn: (Int) -> Unit,
    onNoteOff: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val pressedByPointer = remember { mutableStateMapOf<PointerId, Set<Int>>() }
    val keyColors = LocalKeyColors.current
    val textMeasurer = rememberTextMeasurer()

    val layout = remember(size, lowestNote, whiteKeyCount) {
        if (size.width == 0 || size.height == 0) {
            null
        } else {
            KeyboardLayout(lowestNote, whiteKeyCount, size.width.toFloat(), size.height.toFloat())
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .keyboardPointerInput(
                layout,
                useAreaHitTest,
                areaOverlapThreshold,
                pressedByPointer,
                onNoteOn,
                onNoteOff,
            ),
    ) {
        val current = layout ?: return@Canvas
        val pressed = pressedByPointer.values.flatten().toSet()
        current.whiteKeys.forEach { key ->
            val fill = if (key.midiNote in pressed) keyColors.whiteKeyPressed else keyColors.whiteKey
            drawKey(key, fill, keyColors)
        }
        current.blackKeys.forEach { key ->
            val fill = if (key.midiNote in pressed) keyColors.blackKeyPressed else keyColors.blackKey
            drawKey(key, fill, keyColors)
        }
        if (labelMode != KeyLabelMode.NONE) {
            current.whiteKeys.forEach { key ->
                drawLabel(key, labelMode, textMeasurer, keyColors.whiteKeyLabel)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.keyboardPointerInput(
    layout: KeyboardLayout?,
    useAreaHitTest: Boolean,
    areaOverlapThreshold: Float,
    pressedByPointer: SnapshotStateMap<PointerId, Set<Int>>,
    onNoteOn: (Int) -> Unit,
    onNoteOff: (Int) -> Unit,
): Modifier = pointerInput(layout, useAreaHitTest, areaOverlapThreshold) {
    val current = layout ?: return@pointerInput
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            releaseVanishedPointers(event.changes.mapTo(mutableSetOf()) { it.id }, pressedByPointer, onNoteOff)
            event.changes.forEachIndexed { index, change ->
                if (change.changedToUpIgnoreConsumed()) {
                    pressedByPointer.remove(change.id)?.forEach(onNoteOff)
                } else if (change.changedToDownIgnoreConsumed() || change.positionChanged()) {
                    retarget(
                        current,
                        useAreaHitTest,
                        areaOverlapThreshold,
                        event,
                        index,
                        change,
                        pressedByPointer,
                        onNoteOn,
                        onNoteOff,
                    )
                }
                change.consume()
            }
        }
    }
}

// A pointer that leaves the window never reports an up event; without this its notes would stick.
private fun releaseVanishedPointers(
    live: Set<PointerId>,
    pressedByPointer: SnapshotStateMap<PointerId, Set<Int>>,
    onNoteOff: (Int) -> Unit,
) {
    pressedByPointer.keys.filterNot(live::contains).forEach { stale ->
        pressedByPointer.remove(stale)?.forEach(onNoteOff)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun retarget(
    layout: KeyboardLayout,
    useAreaHitTest: Boolean,
    areaOverlapThreshold: Float,
    event: PointerEvent,
    changeIndex: Int,
    change: PointerInputChange,
    pressedByPointer: SnapshotStateMap<PointerId, Set<Int>>,
    onNoteOn: (Int) -> Unit,
    onNoteOff: (Int) -> Unit,
) {
    val notes = if (useAreaHitTest) {
        val (touchMajor, touchMinor) = touchSize(event, changeIndex)
        layout.notesInArea(change.position.x, change.position.y, touchMajor, touchMinor, areaOverlapThreshold)
    } else {
        setOfNotNull(layout.noteAt(change.position.x, change.position.y))
    }
    val previous = pressedByPointer[change.id] ?: emptySet()
    if (notes == previous) return
    (previous - notes).forEach(onNoteOff)
    (notes - previous).forEach(onNoteOn)
    if (notes.isEmpty()) {
        pressedByPointer.remove(change.id)
    } else {
        pressedByPointer[change.id] = notes
    }
}

// Compose's PointerId is synthetic, not the MotionEvent pointer id; only the changes-list index matches.
@OptIn(ExperimentalComposeUiApi::class)
private fun touchSize(event: PointerEvent, changeIndex: Int): Pair<Float, Float> {
    val motionEvent = event.motionEvent ?: return 0f to 0f
    return if (changeIndex >= motionEvent.pointerCount) {
        0f to 0f
    } else {
        motionEvent.getTouchMajor(changeIndex) to motionEvent.getTouchMinor(changeIndex)
    }
}

private fun DrawScope.drawLabel(
    key: KeyRect,
    labelMode: KeyLabelMode,
    textMeasurer: TextMeasurer,
    color: Color,
) {
    val isC = Note.pitchClass(key.midiNote) == 0
    if (labelMode == KeyLabelMode.C_ONLY && !isC) return
    val text = if (isC) Note.fullName(key.midiNote) else Note.pitchName(key.midiNote)
    val measured = textMeasurer.measure(text, TextStyle(fontSize = LABEL_TEXT_SIZE_SP.sp, color = color))
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            x = key.left + (key.width - measured.size.width) / 2f,
            y = key.bottom - measured.size.height - LABEL_BOTTOM_PADDING,
        ),
    )
}
