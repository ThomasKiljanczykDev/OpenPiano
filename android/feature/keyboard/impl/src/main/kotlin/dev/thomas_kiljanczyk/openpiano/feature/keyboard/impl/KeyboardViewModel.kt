package dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.thomas_kiljanczyk.openpiano.core.audio.AudioEngine
import dev.thomas_kiljanczyk.openpiano.core.data.repository.UserPreferencesRepository
import dev.thomas_kiljanczyk.openpiano.core.data.touch.TouchAreaSupport
import dev.thomas_kiljanczyk.openpiano.core.midi.MidiOutputPort
import dev.thomas_kiljanczyk.openpiano.core.model.Piano
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings mostly shape the layout; the pointer handler calls [AudioEngine] and [MidiOutputPort]
 * directly for notes, so no dispatcher hop or recomposition sits in the latency path. Reverb is
 * the one setting that must still reach the audio engine, so this class forwards it as it changes.
 */
@HiltViewModel
class KeyboardViewModel @Inject constructor(
    private val settingsRepository: UserPreferencesRepository,
    val audioEngine: AudioEngine,
    val midiOutputPort: MidiOutputPort,
    touchAreaSupport: TouchAreaSupport,
) : ViewModel() {

    val uiState: StateFlow<KeyboardUiState> =
        combine(
            settingsRepository.keyboardSettings,
            audioEngine.isReady,
            midiOutputPort.isConnected,
            flowOf(touchAreaSupport.isSupported),
            ::KeyboardUiState,
        )
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = KeyboardUiState(),
            )

    init {
        viewModelScope.launch {
            settingsRepository.keyboardSettings
                .map { it.reverbEnabled }
                .distinctUntilChanged()
                .collect(audioEngine::setReverbEnabled)
        }
    }

    fun shiftKeys(deltaWhiteKeys: Int) {
        viewModelScope.launch {
            val index = Piano.whiteKeyIndexOf(uiState.value.lowestNote) + deltaWhiteKeys
            settingsRepository.setLowestNote(Piano.whiteKeyAt(index))
        }
    }

    fun setLowestNote(midi: Int) {
        viewModelScope.launch { settingsRepository.setLowestNote(midi) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
