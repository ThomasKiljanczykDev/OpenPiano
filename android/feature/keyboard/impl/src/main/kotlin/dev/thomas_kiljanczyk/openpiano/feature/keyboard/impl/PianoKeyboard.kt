package dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl

import android.view.MotionEvent
import androidx.collection.MutableLongSet
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
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
private const val MIDI_NOTE_COUNT = 128

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
    val currentOnNoteOn by rememberUpdatedState(onNoteOn)
    val currentOnNoteOff by rememberUpdatedState(onNoteOff)
    val heldNotes = remember { HeldNotes({ currentOnNoteOn(it) }, { currentOnNoteOff(it) }) }
    val nodeCoordinates = remember { NodeCoordinates() }
    val keyColors = LocalKeyColors.current
    val textMeasurer = rememberTextMeasurer()

    val layout = remember(size, lowestNote, whiteKeyCount) {
        if (size.width == 0 || size.height == 0) {
            null
        } else {
            KeyboardLayout(lowestNote, whiteKeyCount, size.width.toFloat(), size.height.toFloat())
        }
    }

    DisposableEffect(heldNotes, layout) {
        onDispose { heldNotes.releaseAll() }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .onPlaced { nodeCoordinates.value = it }
            .keyboardPointerInput(layout, useAreaHitTest, areaOverlapThreshold, heldNotes, nodeCoordinates),
    ) {
        val current = layout ?: return@Canvas
        val pressed = heldNotes.byPointer.values.flatten().toSet()
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

private class NodeCoordinates {
    var value: LayoutCoordinates? = null
}

/**
 * A note shared by several pointers sounds once and is released only when its last holder lifts.
 */
private class HeldNotes(
    private val onNoteOn: (Int) -> Unit,
    private val onNoteOff: (Int) -> Unit,
) {
    val byPointer: SnapshotStateMap<PointerId, Set<Int>> = mutableStateMapOf()
    private val holders = IntArray(MIDI_NOTE_COUNT)

    // Includes pointers over no key, which byPointer drops.
    private val down = MutableLongSet()

    // Pointers down across a layout change stay silent until lifted, so jitter can't retrigger a shifted key.
    // PointerIds are never reused, so entries of vanished pointers are harmless until everything lifts.
    private val muted = MutableLongSet()

    fun isMuted(id: PointerId): Boolean = id.value in muted

    fun retarget(id: PointerId, notes: Set<Int>) {
        val previous = byPointer[id].orEmpty()
        if (notes == previous) return
        previous.forEach { if (it !in notes) release(it) }
        notes.forEach { if (it !in previous) acquire(it) }
        if (notes.isEmpty()) {
            byPointer.remove(id)
        } else {
            byPointer[id] = notes
        }
    }

    // A pointer that leaves the window never reports an up event; without this its notes would stick.
    fun sync(changes: List<PointerInputChange>) {
        down.clear()
        for (i in changes.indices) {
            val change = changes[i]
            if (change.pressed) down += change.id.value else lift(change.id)
        }
        if (down.isEmpty()) muted.clear()
        if (byPointer.isEmpty()) return
        val iterator = byPointer.iterator()
        while (iterator.hasNext()) {
            val (id, notes) = iterator.next()
            if (id.value !in down) {
                iterator.remove()
                notes.forEach(::release)
            }
        }
    }

    fun releaseAll() {
        muted += down
        byPointer.values.forEach { notes -> notes.forEach(::release) }
        byPointer.clear()
    }

    private fun lift(id: PointerId) {
        muted -= id.value
        byPointer.remove(id)?.forEach(::release)
    }

    private fun acquire(note: Int) {
        if (holders[note]++ == 0) onNoteOn(note)
    }

    private fun release(note: Int) {
        if (holders[note] == 0) return
        if (--holders[note] == 0) onNoteOff(note)
    }
}

private fun Modifier.keyboardPointerInput(
    layout: KeyboardLayout?,
    useAreaHitTest: Boolean,
    areaOverlapThreshold: Float,
    heldNotes: HeldNotes,
    nodeCoordinates: NodeCoordinates,
): Modifier = pointerInput(layout, useAreaHitTest, areaOverlapThreshold, heldNotes) {
    val current = layout ?: return@pointerInput
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val changes = event.changes
            heldNotes.sync(changes)
            // The node resizes a frame before recomposition swaps in the matching layout.
            if (size.width.toFloat() != current.width || size.height.toFloat() != current.height) {
                heldNotes.releaseAll()
            }
            for (i in changes.indices) {
                val change = changes[i]
                val moved = change.changedToDownIgnoreConsumed() || change.positionChanged()
                if (change.pressed && moved && !heldNotes.isMuted(change.id)) {
                    val notes = if (useAreaHitTest) {
                        notesUnderTouch(current, areaOverlapThreshold, event, change, nodeCoordinates.value)
                    } else {
                        setOfNotNull(current.noteAt(change.position.x, change.position.y))
                    }
                    heldNotes.retarget(change.id, notes)
                }
                change.consume()
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun notesUnderTouch(
    layout: KeyboardLayout,
    areaOverlapThreshold: Float,
    event: PointerEvent,
    change: PointerInputChange,
    coordinates: LayoutCoordinates?,
): Set<Int> {
    val x = change.position.x
    val y = change.position.y
    val motionEvent = event.motionEvent
    val index = motionPointerIndex(motionEvent, change, coordinates)
    if (motionEvent == null || index < 0) return layout.notesInArea(x, y, 0f, 0f, areaOverlapThreshold)
    val touchMajor = motionEvent.getTouchMajor(index)
    val touchMinor = motionEvent.getTouchMinor(index)
    return layout.notesInArea(x, y, touchMajor, touchMinor, areaOverlapThreshold)
}

/**
 * Compose's PointerId is synthetic and [PointerEvent.changes] holds only pointers hitting this node,
 * so the MotionEvent pointer is matched by nearest position in root coordinates.
 */
private fun motionPointerIndex(
    motionEvent: MotionEvent?,
    change: PointerInputChange,
    coordinates: LayoutCoordinates?,
): Int {
    if (motionEvent == null || coordinates == null || !coordinates.isAttached) return -1
    val target = coordinates.localToRoot(change.position)
    var best = -1
    var bestDistance = Float.MAX_VALUE
    for (i in 0 until motionEvent.pointerCount) {
        val dx = motionEvent.getX(i) - target.x
        val dy = motionEvent.getY(i) - target.y
        val distance = dx * dx + dy * dy
        if (distance < bestDistance) {
            bestDistance = distance
            best = i
        }
    }
    return best
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
