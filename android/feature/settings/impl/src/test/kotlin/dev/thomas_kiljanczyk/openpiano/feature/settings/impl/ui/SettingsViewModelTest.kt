package dev.thomas_kiljanczyk.openpiano.feature.settings.impl.ui

import app.cash.turbine.test
import dev.thomas_kiljanczyk.openpiano.core.data.model.KeyboardSettings
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.TouchHitTestMode
import dev.thomas_kiljanczyk.openpiano.core.testing.FakeMidiOutputPort
import dev.thomas_kiljanczyk.openpiano.core.testing.FakeTouchAreaSupport
import dev.thomas_kiljanczyk.openpiano.core.testing.FakeUserPreferencesRepository
import dev.thomas_kiljanczyk.openpiano.core.testing.MainDispatcherRule
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LanguageOption
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LanguageOptionsProvider
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LocaleManager
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeUserPreferencesRepository()
    private val midiOutputPort = FakeMidiOutputPort()
    private val localeManager = FakeLocaleManager(LanguageOption("pl"))
    private val options = listOf(LanguageOption.SYSTEM, LanguageOption("pl"))

    private fun viewModel(touchAreaSupported: Boolean = true) = SettingsViewModel(
        languageOptionsProvider = FakeLanguageOptionsProvider(options),
        localeManager = localeManager,
        settingsRepository = repository,
        touchAreaSupport = FakeTouchAreaSupport(touchAreaSupported),
        midiOutputPort = midiOutputPort,
    )

    @Test
    fun `exposes provided language options`() {
        assertEquals(options, viewModel().languageOptions)
    }

    @Test
    fun `initial language is the saved one`() {
        assertEquals(LanguageOption("pl"), viewModel().language)
    }

    @Test
    fun `selectLanguage updates state and persists`() {
        val viewModel = viewModel()
        viewModel.selectLanguage(LanguageOption.SYSTEM)
        assertEquals(LanguageOption.SYSTEM, viewModel.language)
        assertEquals(listOf(LanguageOption.SYSTEM), localeManager.updates)
    }

    @Test
    fun `selectLanguage reflects the locale actually applied`() {
        localeManager.ignoreUpdates = true
        val viewModel = viewModel()
        viewModel.selectLanguage(LanguageOption.SYSTEM)
        assertEquals(LanguageOption("pl"), viewModel.language)
    }

    @Test
    fun `refreshLanguage picks up external locale changes`() {
        val viewModel = viewModel()
        localeManager.saved = LanguageOption("de")
        viewModel.refreshLanguage()
        assertEquals(LanguageOption("de"), viewModel.language)
    }

    @Test
    fun `uiState is null until settings load`() {
        assertNull(viewModel().uiState.value)
    }

    @Test
    fun `uiState reflects repository settings`() = runTest {
        val stored = KeyboardSettings.DEFAULT.copy(visibleWhiteKeys = 14, labelMode = KeyLabelMode.ALL)
        repository.emit(stored)
        viewModel().uiState.filterNotNull().test {
            assertEquals(stored, awaitItem().settings)
        }
    }

    @Test
    fun `uiState reflects midi connection`() = runTest {
        viewModel().uiState.filterNotNull().test {
            assertFalse(awaitItem().midiOutputConnected)
            midiOutputPort.setConnected(true)
            assertTrue(awaitItem().midiOutputConnected)
        }
    }

    @Test
    fun `uiState reports unsupported area mode`() = runTest {
        viewModel(touchAreaSupported = false).uiState.filterNotNull().test {
            assertFalse(awaitItem().areaModeSupported)
        }
    }

    @Test
    fun `setters delegate to repository`() = runTest {
        val viewModel = viewModel()
        val max = KeyboardSettings.VISIBLE_WHITE_KEYS_RANGE.last
        viewModel.setVisibleWhiteKeys(max)
        viewModel.setLabelMode(KeyLabelMode.NONE)
        viewModel.setReverbEnabled(false)
        viewModel.setMidiOutputEnabled(true)
        viewModel.setTouchHitTestMode(TouchHitTestMode.POINT)
        viewModel.setAreaOverlapThresholdPercent(KeyboardSettings.AREA_OVERLAP_THRESHOLD_PERCENT_RANGE.first)

        val expected = KeyboardSettings.DEFAULT.copy(
            visibleWhiteKeys = max,
            labelMode = KeyLabelMode.NONE,
            reverbEnabled = false,
            midiOutputEnabled = true,
            touchHitTestMode = TouchHitTestMode.POINT,
            areaOverlapThresholdPercent = KeyboardSettings.AREA_OVERLAP_THRESHOLD_PERCENT_RANGE.first,
        )
        assertEquals(expected, repository.keyboardSettings.value)
    }

    private class FakeLocaleManager(var saved: LanguageOption) : LocaleManager {
        val updates = mutableListOf<LanguageOption>()
        var ignoreUpdates = false

        override fun getSavedLanguage(): LanguageOption = saved

        override fun updateLanguage(language: LanguageOption) {
            if (!ignoreUpdates) saved = language
            updates += language
        }
    }

    private class FakeLanguageOptionsProvider(private val options: List<LanguageOption>) :
        LanguageOptionsProvider {
        override fun options() = options
    }
}
