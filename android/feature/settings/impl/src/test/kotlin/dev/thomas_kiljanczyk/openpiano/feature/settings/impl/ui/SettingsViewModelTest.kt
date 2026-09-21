package dev.thomas_kiljanczyk.openpiano.feature.settings.impl.ui

import app.cash.turbine.test
import dev.thomas_kiljanczyk.openpiano.core.data.model.KeyboardSettings
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.TouchHitTestMode
import dev.thomas_kiljanczyk.openpiano.core.testing.FakeKeyboardSettingsRepository
import dev.thomas_kiljanczyk.openpiano.core.testing.FakeMidiOutputPort
import dev.thomas_kiljanczyk.openpiano.core.testing.FakeTouchAreaSupport
import dev.thomas_kiljanczyk.openpiano.core.testing.MainDispatcherRule
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LanguageOption
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LanguageOptionsProvider
import dev.thomas_kiljanczyk.openpiano.feature.settings.impl.domain.LocaleManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeKeyboardSettingsRepository()
    private val midiOutputPort = FakeMidiOutputPort()
    private val localeManager = FakeLocaleManager(LanguageOption("pl"))
    private val options = listOf(LanguageOption.SYSTEM to "System default", LanguageOption("pl") to "Polski")

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
    fun `uiState reflects repository settings`() = runTest {
        val stored = KeyboardSettings.DEFAULT.copy(visibleWhiteKeys = 14, labelMode = KeyLabelMode.ALL)
        repository.emit(stored)
        viewModel().uiState.test {
            assertEquals(stored, awaitItem().settings)
        }
    }

    @Test
    fun `uiState reflects midi connection`() = runTest {
        viewModel().uiState.test {
            assertFalse(awaitItem().midiOutputConnected)
            midiOutputPort.setConnected(true)
            assertTrue(awaitItem().midiOutputConnected)
        }
    }

    @Test
    fun `uiState reports unsupported area mode`() = runTest {
        viewModel(touchAreaSupported = false).uiState.test {
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
        assertEquals(expected, repository.settings.value)
    }

    private class FakeLocaleManager(private var saved: LanguageOption) : LocaleManager {
        val updates = mutableListOf<LanguageOption>()

        override fun getSavedLanguage(): LanguageOption = saved

        override fun updateLanguage(language: LanguageOption) {
            saved = language
            updates += language
        }
    }

    private class FakeLanguageOptionsProvider(private val options: List<Pair<LanguageOption, String>>) :
        LanguageOptionsProvider {
        override fun options() = options
    }
}
