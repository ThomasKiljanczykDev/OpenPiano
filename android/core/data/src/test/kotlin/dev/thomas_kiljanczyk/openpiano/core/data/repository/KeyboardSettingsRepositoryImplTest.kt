package dev.thomas_kiljanczyk.openpiano.core.data.repository

import androidx.datastore.core.DataStoreFactory
import app.cash.turbine.test
import dev.thomas_kiljanczyk.openpiano.core.data.model.KeyboardSettings
import dev.thomas_kiljanczyk.openpiano.core.datastore.proto.KeyboardSettingsSerializer
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.Piano
import dev.thomas_kiljanczyk.openpiano.core.model.TouchHitTestMode
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class KeyboardSettingsRepositoryImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun TestScope.repository(): KeyboardSettingsRepository =
        KeyboardSettingsRepositoryImpl(
            DataStoreFactory.create(
                serializer = KeyboardSettingsSerializer,
                scope = backgroundScope,
                produceFile = { temporaryFolder.newFile("keyboard_settings.pb") },
            ),
        )

    @Test
    fun `emits defaults on first read`() = runTest {
        repository().settings.test {
            assertEquals(KeyboardSettings.DEFAULT, awaitItem())
        }
    }

    @Test
    fun `visible white keys round trips`() = runTest {
        val repository = repository()
        repository.setVisibleWhiteKeys(14)

        repository.settings.test {
            assertEquals(14, awaitItem().visibleWhiteKeys)
        }
    }

    @Test
    fun `lowest note round trips`() = runTest {
        val repository = repository()
        repository.setLowestNote(72)

        repository.settings.test {
            assertEquals(72, awaitItem().lowestNote)
        }
    }

    @Test
    fun `label mode round trips`() = runTest {
        val repository = repository()

        for (mode in KeyLabelMode.entries) {
            repository.setLabelMode(mode)
            repository.settings.test {
                assertEquals(mode, awaitItem().labelMode)
            }
        }
    }

    @Test
    fun `reverb enabled defaults to true`() = runTest {
        repository().settings.test {
            assertEquals(true, awaitItem().reverbEnabled)
        }
    }

    @Test
    fun `reverb toggle round trips`() = runTest {
        val repository = repository()

        repository.setReverbEnabled(false)
        repository.settings.test {
            assertEquals(false, awaitItem().reverbEnabled)
        }

        repository.setReverbEnabled(true)
        repository.settings.test {
            assertEquals(true, awaitItem().reverbEnabled)
        }
    }

    @Test
    fun `midi output enabled defaults to false`() = runTest {
        repository().settings.test {
            assertEquals(false, awaitItem().midiOutputEnabled)
        }
    }

    @Test
    fun `midi output toggle round trips`() = runTest {
        val repository = repository()

        repository.setMidiOutputEnabled(true)
        repository.settings.test {
            assertEquals(true, awaitItem().midiOutputEnabled)
        }

        repository.setMidiOutputEnabled(false)
        repository.settings.test {
            assertEquals(false, awaitItem().midiOutputEnabled)
        }
    }

    @Test
    fun `touch hit test mode defaults to area`() = runTest {
        repository().settings.test {
            assertEquals(TouchHitTestMode.AREA, awaitItem().touchHitTestMode)
        }
    }

    @Test
    fun `touch hit test mode round trips`() = runTest {
        val repository = repository()

        for (mode in TouchHitTestMode.entries) {
            repository.setTouchHitTestMode(mode)
            repository.settings.test {
                assertEquals(mode, awaitItem().touchHitTestMode)
            }
        }
    }

    @Test
    fun `area overlap threshold defaults to 35 percent`() = runTest {
        repository().settings.test {
            assertEquals(35, awaitItem().areaOverlapThresholdPercent)
        }
    }

    @Test
    fun `area overlap threshold round trips`() = runTest {
        val repository = repository()

        repository.setAreaOverlapThresholdPercent(45)
        repository.settings.test {
            assertEquals(45, awaitItem().areaOverlapThresholdPercent)
        }
    }

    @Test
    fun `area overlap threshold clamps to range`() = runTest {
        val repository = repository()

        repository.setAreaOverlapThresholdPercent(0)
        repository.settings.test {
            assertEquals(
                KeyboardSettings.AREA_OVERLAP_THRESHOLD_PERCENT_RANGE.first,
                awaitItem().areaOverlapThresholdPercent,
            )
        }

        repository.setAreaOverlapThresholdPercent(999)
        repository.settings.test {
            assertEquals(
                KeyboardSettings.AREA_OVERLAP_THRESHOLD_PERCENT_RANGE.last,
                awaitItem().areaOverlapThresholdPercent,
            )
        }
    }

    @Test
    fun `visible white keys clamps to range`() = runTest {
        val repository = repository()

        repository.setVisibleWhiteKeys(0)
        repository.settings.test {
            assertEquals(KeyboardSettings.VISIBLE_WHITE_KEYS_RANGE.first, awaitItem().visibleWhiteKeys)
        }

        repository.setVisibleWhiteKeys(999)
        repository.settings.test {
            assertEquals(KeyboardSettings.VISIBLE_WHITE_KEYS_RANGE.last, awaitItem().visibleWhiteKeys)
        }
    }

    @Test
    fun `lowest note snaps to a white key inside the piano range`() = runTest {
        val repository = repository()

        repository.setLowestNote(0)
        repository.settings.test {
            assertEquals(Piano.LOWEST_MIDI, awaitItem().lowestNote)
        }

        repository.setLowestNote(61)
        repository.settings.test {
            assertEquals(60, awaitItem().lowestNote)
        }
    }

    @Test
    fun `lowest note stops where the visible window would run off the top`() = runTest {
        val repository = repository()
        repository.setVisibleWhiteKeys(10)
        repository.setLowestNote(Piano.HIGHEST_MIDI)

        repository.settings.test {
            val settings = awaitItem()
            assertEquals(
                Piano.whiteKeyAt(KeyboardSettings.highestWhiteKeyIndex(settings.visibleWhiteKeys)),
                settings.lowestNote,
            )
        }
    }

    @Test
    fun `widening the window pulls the lowest note back into range`() = runTest {
        val repository = repository()
        repository.setVisibleWhiteKeys(KeyboardSettings.VISIBLE_WHITE_KEYS_RANGE.first)
        repository.setLowestNote(Piano.HIGHEST_MIDI)
        repository.setVisibleWhiteKeys(KeyboardSettings.VISIBLE_WHITE_KEYS_RANGE.last)

        repository.settings.test {
            val settings = awaitItem()
            assertEquals(
                Piano.whiteKeyAt(KeyboardSettings.highestWhiteKeyIndex(settings.visibleWhiteKeys)),
                settings.lowestNote,
            )
        }
    }
}
