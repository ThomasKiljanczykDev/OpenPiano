package dev.thomas_kiljanczyk.openpiano.core.data.repository

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import dev.thomas_kiljanczyk.openpiano.core.data.model.KeyboardSettings
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.TouchHitTestMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import dev.thomas_kiljanczyk.openpiano.core.datastore.proto.KeyLabelMode as KeyLabelModeProto
import dev.thomas_kiljanczyk.openpiano.core.datastore.proto.KeyboardSettings as KeyboardSettingsProto
import dev.thomas_kiljanczyk.openpiano.core.datastore.proto.ReverbState as ReverbStateProto
import dev.thomas_kiljanczyk.openpiano.core.datastore.proto.TouchHitTestMode as TouchHitTestModeProto

class KeyboardSettingsRepositoryImpl @Inject constructor(private val dataStore: DataStore<KeyboardSettingsProto>) :
    KeyboardSettingsRepository {

    override val settings: Flow<KeyboardSettings> = dataStore.data
        .catch { throwable ->
            if (throwable is CorruptionException) {
                emit(KeyboardSettingsProto.getDefaultInstance())
            } else {
                throw throwable
            }
        }
        .map(KeyboardSettingsProto::toDomain)

    override suspend fun setVisibleWhiteKeys(count: Int) {
        val clamped = count.coerceIn(KeyboardSettings.VISIBLE_WHITE_KEYS_RANGE)
        dataStore.updateData { stored ->
            stored.toBuilder()
                .setVisibleWhiteKeys(clamped)
                .setLowestNote(KeyboardSettings.clampLowestNote(stored.toDomain().lowestNote, clamped))
                .build()
        }
    }

    override suspend fun setLowestNote(midi: Int) {
        dataStore.updateData { stored ->
            val visible = stored.toDomain().visibleWhiteKeys
            stored.toBuilder()
                .setLowestNote(KeyboardSettings.clampLowestNote(midi, visible))
                .build()
        }
    }

    override suspend fun setLabelMode(mode: KeyLabelMode) {
        dataStore.updateData { it.toBuilder().setLabelMode(mode.toProto()).build() }
    }

    override suspend fun setReverbEnabled(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setReverbState(enabled.toReverbStateProto()).build() }
    }

    override suspend fun setMidiOutputEnabled(enabled: Boolean) {
        dataStore.updateData { it.toBuilder().setMidiOutputEnabled(enabled).build() }
    }

    override suspend fun setTouchHitTestMode(mode: TouchHitTestMode) {
        dataStore.updateData { it.toBuilder().setTouchHitTestMode(mode.toProto()).build() }
    }

    override suspend fun setAreaOverlapThresholdPercent(percent: Int) {
        val clamped = percent.coerceIn(KeyboardSettings.AREA_OVERLAP_THRESHOLD_PERCENT_RANGE)
        dataStore.updateData { it.toBuilder().setAreaOverlapThresholdPercent(clamped).build() }
    }
}

private fun KeyboardSettingsProto.toDomain(): KeyboardSettings {
    val defaults = KeyboardSettings.DEFAULT
    val visible = visibleWhiteKeys.takeIf { it != 0 } ?: defaults.visibleWhiteKeys
    val lowest = lowestNote.takeIf { it != 0 } ?: defaults.lowestNote
    return KeyboardSettings(
        visibleWhiteKeys = visible,
        lowestNote = KeyboardSettings.clampLowestNote(lowest, visible),
        labelMode = labelMode.toDomain() ?: defaults.labelMode,
        reverbEnabled = reverbState.toDomain() ?: defaults.reverbEnabled,
        midiOutputEnabled = midiOutputEnabled,
        touchHitTestMode = touchHitTestMode.toDomain() ?: defaults.touchHitTestMode,
        areaOverlapThresholdPercent = areaOverlapThresholdPercent.takeIf { it != 0 }
            ?: defaults.areaOverlapThresholdPercent,
    )
}

private fun KeyLabelModeProto.toDomain(): KeyLabelMode? = when (this) {
    KeyLabelModeProto.LABEL_NONE -> KeyLabelMode.NONE
    KeyLabelModeProto.LABEL_C_ONLY -> KeyLabelMode.C_ONLY
    KeyLabelModeProto.LABEL_ALL -> KeyLabelMode.ALL
    KeyLabelModeProto.LABEL_UNSPECIFIED, KeyLabelModeProto.UNRECOGNIZED -> null
}

private fun KeyLabelMode.toProto(): KeyLabelModeProto = when (this) {
    KeyLabelMode.NONE -> KeyLabelModeProto.LABEL_NONE
    KeyLabelMode.C_ONLY -> KeyLabelModeProto.LABEL_C_ONLY
    KeyLabelMode.ALL -> KeyLabelModeProto.LABEL_ALL
}

private fun ReverbStateProto.toDomain(): Boolean? = when (this) {
    ReverbStateProto.REVERB_ON -> true
    ReverbStateProto.REVERB_OFF -> false
    ReverbStateProto.REVERB_UNSPECIFIED, ReverbStateProto.UNRECOGNIZED -> null
}

private fun Boolean.toReverbStateProto(): ReverbStateProto =
    if (this) ReverbStateProto.REVERB_ON else ReverbStateProto.REVERB_OFF

private fun TouchHitTestModeProto.toDomain(): TouchHitTestMode? = when (this) {
    TouchHitTestModeProto.TOUCH_HIT_TEST_MODE_POINT -> TouchHitTestMode.POINT
    TouchHitTestModeProto.TOUCH_HIT_TEST_MODE_AREA -> TouchHitTestMode.AREA
    TouchHitTestModeProto.TOUCH_HIT_TEST_MODE_UNSPECIFIED, TouchHitTestModeProto.UNRECOGNIZED -> null
}

private fun TouchHitTestMode.toProto(): TouchHitTestModeProto = when (this) {
    TouchHitTestMode.POINT -> TouchHitTestModeProto.TOUCH_HIT_TEST_MODE_POINT
    TouchHitTestMode.AREA -> TouchHitTestModeProto.TOUCH_HIT_TEST_MODE_AREA
}
