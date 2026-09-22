package dev.thomas_kiljanczyk.openpiano.feature.settings.impl.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.thomas_kiljanczyk.openpiano.core.data.repository.KeyboardSettingsRepository
import dev.thomas_kiljanczyk.openpiano.core.data.touch.TouchAreaSupport
import dev.thomas_kiljanczyk.openpiano.core.midi.MidiOutputPort
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.TouchHitTestMode
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LanguageOption
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LanguageOptionsProvider
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LocaleManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    languageOptionsProvider: LanguageOptionsProvider,
    private val localeManager: LocaleManager,
    private val settingsRepository: KeyboardSettingsRepository,
    touchAreaSupport: TouchAreaSupport,
    midiOutputPort: MidiOutputPort,
) : ViewModel() {

    val languageOptions: List<Pair<LanguageOption, String>> = languageOptionsProvider.options()

    var language: LanguageOption by mutableStateOf(localeManager.getSavedLanguage())
        private set

    val uiState: StateFlow<SettingsUiState> =
        combine(
            settingsRepository.settings,
            midiOutputPort.isConnected,
            flowOf(touchAreaSupport.isSupported),
            ::SettingsUiState,
        )
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = SettingsUiState(),
            )

    fun selectLanguage(option: LanguageOption) {
        language = option
        localeManager.updateLanguage(option)
    }

    fun setLabelMode(mode: KeyLabelMode) {
        viewModelScope.launch { settingsRepository.setLabelMode(mode) }
    }

    fun setVisibleWhiteKeys(count: Int) {
        viewModelScope.launch { settingsRepository.setVisibleWhiteKeys(count) }
    }

    fun setReverbEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setReverbEnabled(enabled) }
    }

    fun setMidiOutputEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMidiOutputEnabled(enabled) }
    }

    fun setTouchHitTestMode(mode: TouchHitTestMode) {
        viewModelScope.launch { settingsRepository.setTouchHitTestMode(mode) }
    }

    fun setAreaOverlapThresholdPercent(percent: Int) {
        viewModelScope.launch { settingsRepository.setAreaOverlapThresholdPercent(percent) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
