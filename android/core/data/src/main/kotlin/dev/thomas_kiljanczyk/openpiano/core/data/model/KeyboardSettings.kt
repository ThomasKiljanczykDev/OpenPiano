package dev.thomas_kiljanczyk.openpiano.core.data.model

import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.Note
import dev.thomas_kiljanczyk.openpiano.core.model.Piano
import dev.thomas_kiljanczyk.openpiano.core.model.TouchHitTestMode

data class KeyboardSettings(
    val visibleWhiteKeys: Int,
    val lowestNote: Int,
    val labelMode: KeyLabelMode,
    val reverbEnabled: Boolean,
    val midiOutputEnabled: Boolean,
    val touchHitTestMode: TouchHitTestMode,
    val areaOverlapThresholdPercent: Int,
) {
    companion object {
        val VISIBLE_WHITE_KEYS_RANGE = 7..21
        val AREA_OVERLAP_THRESHOLD_PERCENT_RANGE = 10..50

        val DEFAULT = KeyboardSettings(
            visibleWhiteKeys = 10,
            lowestNote = Note.firstOfOctave(3),
            labelMode = KeyLabelMode.C_ONLY,
            reverbEnabled = true,
            midiOutputEnabled = false,
            touchHitTestMode = TouchHitTestMode.AREA,
            areaOverlapThresholdPercent = 35,
        )

        fun highestWhiteKeyIndex(visibleWhiteKeys: Int): Int =
            Piano.whiteKeyCount - visibleWhiteKeys

        fun clampLowestNote(midi: Int, visibleWhiteKeys: Int): Int {
            val index = Piano.whiteKeyIndexOf(midi)
                .coerceIn(0, highestWhiteKeyIndex(visibleWhiteKeys))
            return Piano.whiteKeyAt(index)
        }
    }
}
