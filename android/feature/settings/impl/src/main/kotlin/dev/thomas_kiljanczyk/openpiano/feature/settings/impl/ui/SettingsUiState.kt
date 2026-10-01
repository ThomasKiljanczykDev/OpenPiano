package dev.thomas_kiljanczyk.openpiano.feature.settings.impl.ui

import dev.thomas_kiljanczyk.openpiano.core.data.model.KeyboardSettings
import dev.thomas_kiljanczyk.openpiano.core.model.ThemeMode

data class SettingsUiState(
    val settings: KeyboardSettings = KeyboardSettings.DEFAULT,
    val midiOutputConnected: Boolean = false,
    val areaModeSupported: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)
