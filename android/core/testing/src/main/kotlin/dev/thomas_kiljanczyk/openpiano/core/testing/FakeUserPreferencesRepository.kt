package dev.thomas_kiljanczyk.openpiano.core.testing

import dev.thomas_kiljanczyk.openpiano.core.data.model.KeyboardSettings
import dev.thomas_kiljanczyk.openpiano.core.data.repository.UserPreferencesRepository
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.TouchHitTestMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeUserPreferencesRepository(initial: KeyboardSettings = KeyboardSettings.DEFAULT) :
    UserPreferencesRepository {
    private val state = MutableStateFlow(initial)
    private var languageTag: String? = null

    override val keyboardSettings: StateFlow<KeyboardSettings> = state.asStateFlow()

    fun emit(value: KeyboardSettings) {
        state.value = value
    }

    override suspend fun setVisibleWhiteKeys(count: Int) = state.update { it.copy(visibleWhiteKeys = count) }

    override suspend fun setLowestNote(midi: Int) = state.update { it.copy(lowestNote = midi) }

    override suspend fun setLabelMode(mode: KeyLabelMode) = state.update { it.copy(labelMode = mode) }

    override suspend fun setReverbEnabled(enabled: Boolean) = state.update { it.copy(reverbEnabled = enabled) }

    override suspend fun setMidiOutputEnabled(enabled: Boolean) =
        state.update { it.copy(midiOutputEnabled = enabled) }

    override suspend fun setTouchHitTestMode(mode: TouchHitTestMode) =
        state.update { it.copy(touchHitTestMode = mode) }

    override suspend fun setAreaOverlapThresholdPercent(percent: Int) =
        state.update { it.copy(areaOverlapThresholdPercent = percent) }

    override suspend fun getLanguageTag(): String? = languageTag

    override suspend fun setLanguageTag(tag: String?) {
        languageTag = tag
    }
}
