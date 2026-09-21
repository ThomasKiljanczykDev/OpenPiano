package dev.thomas_kiljanczyk.openpiano.tools.screenshotmocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.thomas_kiljanczyk.openpiano.core.designsystem.theme.KeyColors
import dev.thomas_kiljanczyk.openpiano.core.designsystem.theme.LocalKeyColors
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.KeyRect
import dev.thomas_kiljanczyk.openpiano.core.model.KeyboardLayout
import dev.thomas_kiljanczyk.openpiano.core.model.Note
import dev.thomas_kiljanczyk.openpiano.core.model.Piano
import dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl.R

private const val OVERVIEW_HEIGHT_DP = 48
private const val LABEL_TEXT_SIZE_SP = 11
private const val LABEL_BOTTOM_PADDING = 12f
private const val KEY_BORDER_WIDTH = 2f
private const val VIEWPORT_FILL_ALPHA = 0.25f
private const val VIEWPORT_BORDER_WIDTH = 3f

val MOCK_LOWEST_NOTE = Note.firstOfOctave(3)
const val MOCK_VISIBLE_WHITE_KEYS = 10

// Real canvases size via onSizeChanged, which needs a second pass the renderer never runs.
@Composable
fun MockKeyboardScreen(lowestNote: Int, whiteKeyCount: Int, labelMode: KeyLabelMode) {
    val keyColors = LocalKeyColors.current
    val viewportColor = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = Note.fullName(lowestNote), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {}) {
                Icon(
                    ImageVector.vectorResource(R.drawable.settings),
                    contentDescription = stringResource(R.string.keyboard_settings),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(OVERVIEW_HEIGHT_DP.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {}) {
                Icon(ImageVector.vectorResource(R.drawable.keyboard_double_arrow_left), contentDescription = null)
            }
            IconButton(onClick = {}) {
                Icon(ImageVector.vectorResource(R.drawable.keyboard_arrow_left), contentDescription = null)
            }
            Canvas(modifier = Modifier.weight(1f).fillMaxSize().padding(horizontal = 4.dp, vertical = 4.dp)) {
                drawMockOverview(lowestNote, whiteKeyCount, keyColors, viewportColor)
            }
            IconButton(onClick = {}) {
                Icon(ImageVector.vectorResource(R.drawable.keyboard_arrow_right), contentDescription = null)
            }
            IconButton(onClick = {}) {
                Icon(ImageVector.vectorResource(R.drawable.keyboard_double_arrow_right), contentDescription = null)
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawMockKeyboard(lowestNote, whiteKeyCount, labelMode, keyColors, textMeasurer)
            }
        }
    }
}

private fun DrawScope.drawMockKeyboard(
    lowestNote: Int,
    whiteKeyCount: Int,
    labelMode: KeyLabelMode,
    keyColors: KeyColors,
    textMeasurer: TextMeasurer,
) {
    if (size.width <= 0f || size.height <= 0f) return
    val layout = KeyboardLayout(lowestNote, whiteKeyCount, size.width, size.height)
    layout.whiteKeys.forEach { key -> drawMockKey(key, keyColors.whiteKey, keyColors) }
    layout.blackKeys.forEach { key -> drawMockKey(key, keyColors.blackKey, keyColors) }
    if (labelMode != KeyLabelMode.NONE) {
        layout.whiteKeys.forEach { key -> drawMockLabel(key, labelMode, textMeasurer, keyColors.whiteKeyLabel) }
    }
}

private fun DrawScope.drawMockOverview(
    lowestNote: Int,
    visibleWhiteKeys: Int,
    keyColors: KeyColors,
    viewportColor: Color,
) {
    if (size.width <= 0f || size.height <= 0f) return
    val layout = KeyboardLayout(Piano.LOWEST_MIDI, Piano.whiteKeyCount, size.width, size.height)
    layout.whiteKeys.forEach { key -> drawMockKey(key, keyColors.whiteKey, keyColors) }
    layout.blackKeys.forEach { key -> drawMockKey(key, keyColors.blackKey, keyColors) }
    val first = Piano.whiteKeyIndexOf(lowestNote)
    val last = (first + visibleWhiteKeys - 1).coerceAtMost(layout.whiteKeys.lastIndex)
    val left = layout.whiteKeys[first].left
    val viewport = Size(layout.whiteKeys[last].right - left, size.height)
    drawRect(color = viewportColor.copy(alpha = VIEWPORT_FILL_ALPHA), topLeft = Offset(left, 0f), size = viewport)
    drawRect(
        color = viewportColor,
        topLeft = Offset(left, 0f),
        size = viewport,
        style = Stroke(width = VIEWPORT_BORDER_WIDTH),
    )
}

private fun DrawScope.drawMockKey(key: KeyRect, fill: Color, colors: KeyColors) {
    val topLeft = Offset(key.left, key.top)
    val keySize = Size(key.width, key.height)
    drawRect(color = fill, topLeft = topLeft, size = keySize)
    drawRect(
        color = colors.keyBorder,
        topLeft = topLeft,
        size = keySize,
        style = Stroke(width = KEY_BORDER_WIDTH),
    )
}

private fun DrawScope.drawMockLabel(
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
