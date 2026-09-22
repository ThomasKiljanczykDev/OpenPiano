package dev.thomas_kiljanczyk.openpiano.feature.settings.impl.ui

import dev.thomas_kiljanczyk.openpiano.core.data.model.KeyboardSettings

data class SettingsUiState(
    val settings: KeyboardSettings = KeyboardSettings.DEFAULT,
    val midiOutputConnected: Boolean = false,
    val areaModeSupported: Boolean = true,
)
