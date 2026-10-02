package dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.thomas_kiljanczyk.openpiano.core.designsystem.theme.LocalKeyColors
import dev.thomas_kiljanczyk.openpiano.core.model.KeyboardLayout
import dev.thomas_kiljanczyk.openpiano.core.model.Piano

private const val VIEWPORT_FILL_ALPHA = 0.15f
private const val OUTSIDE_SCRIM_ALPHA = 0.55f
private val ViewportBorderWidth = 3.dp
private val ViewportCornerRadius = 4.dp

/**
 * Miniature of the whole 88-key piano with the played window drawn over it. Touching or dragging
 * centres the window on the finger, so the overview doubles as the scrollbar for [PianoKeyboard].
 */
@Composable
fun KeyboardOverview(
    lowestNote: Int,
    visibleWhiteKeys: Int,
    onLowestNoteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val keyColors = LocalKeyColors.current
    val viewportColor = MaterialTheme.colorScheme.primary
    val scrimColor = MaterialTheme.colorScheme.scrim
    val currentOnLowestNoteChange by rememberUpdatedState(onLowestNoteChange)

    val layout = remember(size) {
        if (size.width == 0 || size.height == 0) {
            null
        } else {
            KeyboardLayout(
                lowestNote = Piano.LOWEST_MIDI,
                whiteKeyCount = Piano.whiteKeyCount,
                width = size.width.toFloat(),
                height = size.height.toFloat(),
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .overviewPointerInput(layout, visibleWhiteKeys) { currentOnLowestNoteChange(it) },
    ) {
        val current = layout ?: return@Canvas
        current.whiteKeys.forEach { drawKey(it, keyColors.whiteKey, keyColors) }
        current.blackKeys.forEach { drawKey(it, keyColors.blackKey, keyColors) }

        val first = Piano.whiteKeyIndexOf(lowestNote)
        val last = (first + visibleWhiteKeys - 1).coerceAtMost(current.whiteKeys.lastIndex)
        val left = current.whiteKeys[first].left
        val right = current.whiteKeys[last].right
        val scrim = scrimColor.copy(alpha = OUTSIDE_SCRIM_ALPHA)
        drawRect(color = scrim, size = Size(left, this.size.height))
        drawRect(
            color = scrim,
            topLeft = Offset(right, 0f),
            size = Size(this.size.width - right, this.size.height),
        )

        val stroke = ViewportBorderWidth.toPx()
        val corner = CornerRadius(ViewportCornerRadius.toPx())
        // Inset by half the stroke so the border isn't clipped at the canvas edges.
        val inset = stroke / 2
        val topLeft = Offset(left + inset, inset)
        val viewport = Size(right - left - stroke, this.size.height - stroke)
        drawRoundRect(
            color = viewportColor.copy(alpha = VIEWPORT_FILL_ALPHA),
            topLeft = topLeft,
            size = viewport,
            cornerRadius = corner,
        )
        drawRoundRect(
            color = viewportColor,
            topLeft = topLeft,
            size = viewport,
            cornerRadius = corner,
            style = Stroke(width = stroke),
        )
    }
}

private fun Modifier.overviewPointerInput(
    layout: KeyboardLayout?,
    visibleWhiteKeys: Int,
    onLowestNoteChange: (Int) -> Unit,
): Modifier = pointerInput(layout, visibleWhiteKeys) {
    val current = layout ?: return@pointerInput
    val highest = Piano.whiteKeyCount - visibleWhiteKeys
    awaitPointerEventScope {
        var emitted = -1
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.pressed }
            if (change == null) {
                emitted = -1
                continue
            }
            val touched = (change.position.x / current.whiteKeyWidth).toInt()
            val index = (touched - visibleWhiteKeys / 2).coerceIn(0, highest)
            val note = Piano.whiteKeyAt(index)
            if (note != emitted) {
                emitted = note
                onLowestNoteChange(note)
            }
            change.consume()
        }
    }
}
