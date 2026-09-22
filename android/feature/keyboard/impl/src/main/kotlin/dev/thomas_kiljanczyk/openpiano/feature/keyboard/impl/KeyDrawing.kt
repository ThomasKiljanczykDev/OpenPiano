package dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.thomas_kiljanczyk.openpiano.core.designsystem.theme.KeyColors
import dev.thomas_kiljanczyk.openpiano.core.model.KeyRect

internal const val KEY_BORDER_WIDTH = 1.5f

internal fun DrawScope.drawKey(key: KeyRect, fill: Color, colors: KeyColors) {
    val topLeft = Offset(key.left, key.top)
    val size = Size(key.width, key.height)
    drawRect(color = fill, topLeft = topLeft, size = size)
    drawRect(
        color = colors.keyBorder,
        topLeft = topLeft,
        size = size,
        style = Stroke(width = KEY_BORDER_WIDTH),
    )
}
