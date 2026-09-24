package dev.thomas_kiljanczyk.openpiano.core.data.repository

import dev.thomas_kiljanczyk.openpiano.core.data.model.KeyboardSettings
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.TouchHitTestMode
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {

    val keyboardSettings: Flow<KeyboardSettings>

    suspend fun setVisibleWhiteKeys(count: Int)

    suspend fun setLowestNote(midi: Int)

    suspend fun setLabelMode(mode: KeyLabelMode)

    suspend fun setReverbEnabled(enabled: Boolean)

    suspend fun setMidiOutputEnabled(enabled: Boolean)

    suspend fun setTouchHitTestMode(mode: TouchHitTestMode)

    suspend fun setAreaOverlapThresholdPercent(percent: Int)

    /** Null when the user never picked a language. */
    suspend fun getLanguageTag(): String?

    suspend fun setLanguageTag(tag: String?)
}
