package dev.thomas_kiljanczyk.openpiano.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class KeyColors(
    val whiteKey: Color,
    val whiteKeyPressed: Color,
    val blackKey: Color,
    val blackKeyPressed: Color,
    val keyBorder: Color,
    val whiteKeyLabel: Color,
    val blackKeyLabel: Color,
)

internal val LightKeyColors = KeyColors(
    whiteKey = Ivory,
    whiteKeyPressed = AshGray,
    blackKey = Ebony,
    blackKeyPressed = SlateGray,
    keyBorder = Color(0xFF8A8A8A),
    whiteKeyLabel = SlateGray,
    blackKeyLabel = AshGray,
)

internal val DarkKeyColors = LightKeyColors.copy(
    whiteKey = Color(0xFFE3DFD8),
    keyBorder = Color(0xFF4A4A4A),
)

val LocalKeyColors = staticCompositionLocalOf { LightKeyColors }
