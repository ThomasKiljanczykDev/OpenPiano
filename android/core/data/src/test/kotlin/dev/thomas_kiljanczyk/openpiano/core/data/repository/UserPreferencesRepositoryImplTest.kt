package dev.thomas_kiljanczyk.openpiano.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import app.cash.turbine.test
import dev.thomas_kiljanczyk.openpiano.core.data.model.KeyboardSettings
import dev.thomas_kiljanczyk.openpiano.core.datastore.proto.UserPreferences
import dev.thomas_kiljanczyk.openpiano.core.datastore.proto.UserPreferencesSerializer
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.Piano
import dev.thomas_kiljanczyk.openpiano.core.model.TouchHitTestMode
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class UserPreferencesRepositoryImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun TestScope.dataStore(): DataStore<UserPreferences> = DataStoreFactory.create(
        serializer = UserPreferencesSerializer,
        scope = backgroundScope,
        produceFile = { temporaryFolder.newFile("user_preferences.pb") },
    )

    private fun TestScope.repository(): UserPreferencesRepository = UserPreferencesRepositoryImpl(dataStore())

    @Test
    fun `emits defaults on first read`() = runTest {
        repository().keyboardSettings.test {
            assertEquals(KeyboardSettings.DEFAULT, awaitItem())
        }
    }

    @Test
    fun `visible white keys round trips`() = runTest {
        val repository = repository()
        repository.setVisibleWhiteKeys(14)

        repository.keyboardSettings.test {
            assertEquals(14, awaitItem().visibleWhiteKeys)
        }
    }

    @Test
    fun `lowest note round trips`() = runTest {
        val repository = repository()
        repository.setLowestNote(72)

        repository.keyboardSettings.test {
            assertEquals(72, awaitItem().lowestNote)
        }
    }

    @Test
    fun `label mode round trips`() = runTest {
        val repository = repository()

        for (mode in KeyLabelMode.entries) {
            repository.setLabelMode(mode)
            repository.keyboardSettings.test {
                assertEquals(mode, awaitItem().labelMode)
            }
        }
    }

    @Test
    fun `reverb enabled defaults to true`() = runTest {
        repository().keyboardSettings.test {
            assertEquals(true, awaitItem().reverbEnabled)
        }
    }

    @Test
    fun `reverb toggle round trips`() = runTest {
        val repository = repository()

        repository.setReverbEnabled(false)
        repository.keyboardSettings.test {
            assertEquals(false, awaitItem().reverbEnabled)
        }

        repository.setReverbEnabled(true)
        repository.keyboardSettings.test {
            assertEquals(true, awaitItem().reverbEnabled)
        }
    }

    @Test
    fun `midi output enabled defaults to false`() = runTest {
        repository().keyboardSettings.test {
            assertEquals(false, awaitItem().midiOutputEnabled)
        }
    }

    @Test
    fun `midi output toggle round trips`() = runTest {
        val repository = repository()

        repository.setMidiOutputEnabled(true)
        repository.keyboardSettings.test {
            assertEquals(true, awaitItem().midiOutputEnabled)
        }

        repository.setMidiOutputEnabled(false)
        repository.keyboardSettings.test {
            assertEquals(false, awaitItem().midiOutputEnabled)
        }
    }

    @Test
    fun `touch hit test mode defaults to area`() = runTest {
        repository().keyboardSettings.test {
            assertEquals(TouchHitTestMode.AREA, awaitItem().touchHitTestMode)
        }
    }

    @Test
    fun `touch hit test mode round trips`() = runTest {
        val repository = repository()

        for (mode in TouchHitTestMode.entries) {
            repository.setTouchHitTestMode(mode)
            repository.keyboardSettings.test {
                assertEquals(mode, awaitItem().touchHitTestMode)
            }
        }
    }

    @Test
    fun `area overlap threshold defaults to 35 percent`() = runTest {
        repository().keyboardSettings.test {
            assertEquals(35, awaitItem().areaOverlapThresholdPercent)
        }
    }

    @Test
    fun `area overlap threshold round trips`() = runTest {
        val repository = repository()

        repository.setAreaOverlapThresholdPercent(45)
        repository.keyboardSettings.test {
            assertEquals(45, awaitItem().areaOverlapThresholdPercent)
        }
    }

    @Test
    fun `area overlap threshold clamps to range`() = runTest {
        val repository = repository()

        repository.setAreaOverlapThresholdPercent(0)
        repository.keyboardSettings.test {
            assertEquals(
                KeyboardSettings.AREA_OVERLAP_THRESHOLD_PERCENT_RANGE.first,
                awaitItem().areaOverlapThresholdPercent,
            )
        }

        repository.setAreaOverlapThresholdPercent(999)
        repository.keyboardSettings.test {
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
        repository.keyboardSettings.test {
            assertEquals(KeyboardSettings.VISIBLE_WHITE_KEYS_RANGE.first, awaitItem().visibleWhiteKeys)
        }

        repository.setVisibleWhiteKeys(999)
        repository.keyboardSettings.test {
            assertEquals(KeyboardSettings.VISIBLE_WHITE_KEYS_RANGE.last, awaitItem().visibleWhiteKeys)
        }
    }

    @Test
    fun `lowest note snaps to a white key inside the piano range`() = runTest {
        val repository = repository()

        repository.setLowestNote(0)
        repository.keyboardSettings.test {
            assertEquals(Piano.LOWEST_MIDI, awaitItem().lowestNote)
        }

        repository.setLowestNote(61)
        repository.keyboardSettings.test {
            assertEquals(60, awaitItem().lowestNote)
        }
    }

    @Test
    fun `lowest note stops where the visible window would run off the top`() = runTest {
        val repository = repository()
        repository.setVisibleWhiteKeys(10)
        repository.setLowestNote(Piano.HIGHEST_MIDI)

        repository.keyboardSettings.test {
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

        repository.keyboardSettings.test {
            val settings = awaitItem()
            assertEquals(
                Piano.whiteKeyAt(KeyboardSettings.highestWhiteKeyIndex(settings.visibleWhiteKeys)),
                settings.lowestNote,
            )
        }
    }

    @Test
    fun `out of range stored values are clamped on read`() = runTest {
        val dataStore = dataStore()
        dataStore.updateData {
            it.toBuilder()
                .setVisibleWhiteKeys(999)
                .setLowestNote(Piano.HIGHEST_MIDI)
                .setAreaOverlapThresholdPercent(999)
                .build()
        }

        UserPreferencesRepositoryImpl(dataStore).keyboardSettings.test {
            val settings = awaitItem()
            assertEquals(KeyboardSettings.VISIBLE_WHITE_KEYS_RANGE.last, settings.visibleWhiteKeys)
            assertEquals(
                Piano.whiteKeyAt(KeyboardSettings.highestWhiteKeyIndex(settings.visibleWhiteKeys)),
                settings.lowestNote,
            )
            assertEquals(
                KeyboardSettings.AREA_OVERLAP_THRESHOLD_PERCENT_RANGE.last,
                settings.areaOverlapThresholdPercent,
            )
        }
    }

    @Test
    fun `read failure emits defaults then recovers once reads succeed`() = runTest {
        val stored = UserPreferences.newBuilder().setVisibleWhiteKeys(14).build()
        var failuresLeft = 2
        val flaky = object : DataStore<UserPreferences> {
            override val data: Flow<UserPreferences> = flow {
                if (failuresLeft > 0) {
                    failuresLeft--
                    throw IOException("disk")
                }
                emit(stored)
                awaitCancellation()
            }

            override suspend fun updateData(
                transform: suspend (t: UserPreferences) -> UserPreferences,
            ): UserPreferences = throw IOException("disk")
        }

        UserPreferencesRepositoryImpl(flaky).keyboardSettings.test {
            assertEquals(KeyboardSettings.DEFAULT, awaitItem())
            assertEquals(14, awaitItem().visibleWhiteKeys)
        }
    }

    @Test
    fun `write failure does not throw`() = runTest {
        val failing = object : DataStore<UserPreferences> {
            override val data: Flow<UserPreferences> = flow { emit(UserPreferences.getDefaultInstance()) }

            override suspend fun updateData(
                transform: suspend (t: UserPreferences) -> UserPreferences,
            ): UserPreferences = throw IOException("disk")
        }

        UserPreferencesRepositoryImpl(failing).setReverbEnabled(false)
    }

    @Test
    fun `language tag is null until set`() = runTest {
        assertNull(repository().getLanguageTag())
    }

    @Test
    fun `language tag round trips and clears`() = runTest {
        val repository = repository()
        repository.setLanguageTag("pl")
        assertEquals("pl", repository.getLanguageTag())

        repository.setLanguageTag(null)
        assertNull(repository.getLanguageTag())
    }

    @Test
    fun `language change does not re-emit keyboard settings`() = runTest {
        val repository = repository()
        repository.keyboardSettings.test {
            awaitItem()
            repository.setLanguageTag("de")
            expectNoEvents()
        }
    }
}
