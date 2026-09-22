package dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl

import dev.thomas_kiljanczyk.openpiano.core.data.model.KeyboardSettings
import dev.thomas_kiljanczyk.openpiano.core.model.Piano
import dev.thomas_kiljanczyk.openpiano.core.model.TouchHitTestMode

data class KeyboardUiState(
    val settings: KeyboardSettings = KeyboardSettings.DEFAULT,
    val soundReady: Boolean = false,
    val midiOutputConnected: Boolean = false,
    val areaModeSupported: Boolean = true,
) {
    val lowestNote: Int get() = settings.lowestNote
    val canShiftDown: Boolean get() = Piano.whiteKeyIndexOf(lowestNote) > 0
    val canShiftUp: Boolean
        get() = Piano.whiteKeyIndexOf(lowestNote) <
            KeyboardSettings.highestWhiteKeyIndex(settings.visibleWhiteKeys)
    val useAreaHitTest: Boolean
        get() = areaModeSupported && settings.touchHitTestMode == TouchHitTestMode.AREA
    val areaOverlapThreshold: Float
        get() = settings.areaOverlapThresholdPercent / PERCENT_SCALE
}

private const val PERCENT_SCALE = 100f
