package dev.thomas_kiljanczyk.openpiano.core.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import dev.thomas_kiljanczyk.openpiano.core.data.model.KeyboardSettings
import dev.thomas_kiljanczyk.openpiano.core.datastore.proto.UserPreferences
import dev.thomas_kiljanczyk.openpiano.core.model.KeyLabelMode
import dev.thomas_kiljanczyk.openpiano.core.model.TouchHitTestMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException
import javax.inject.Inject
import dev.thomas_kiljanczyk.openpiano.core.datastore.proto.KeyLabelMode as KeyLabelModeProto
import dev.thomas_kiljanczyk.openpiano.core.datastore.proto.ReverbState as ReverbStateProto
import dev.thomas_kiljanczyk.openpiano.core.datastore.proto.TouchHitTestMode as TouchHitTestModeProto

class UserPreferencesRepositoryImpl @Inject constructor(private val dataStore: DataStore<UserPreferences>) :
    UserPreferencesRepository {

    override val keyboardSettings: Flow<KeyboardSettings> = flow {
        var emittedAny = false
        dataStore.data
            .retryWhen { cause, attempt ->
                if (cause !is IOException) return@retryWhen false
                Log.w(TAG, "Reading keyboard settings failed", cause)
                if (!emittedAny) emit(UserPreferences.getDefaultInstance())
                delay(retryDelayMillis(attempt))
                true
            }
            .collect {
                emittedAny = true
                emit(it)
            }
    }.map(UserPreferences::toDomain).distinctUntilChanged()

    override suspend fun setVisibleWhiteKeys(count: Int) {
        val clamped = count.coerceIn(KeyboardSettings.VISIBLE_WHITE_KEYS_RANGE)
        update { stored ->
            stored.toBuilder()
                .setVisibleWhiteKeys(clamped)
                .setLowestNote(KeyboardSettings.clampLowestNote(stored.toDomain().lowestNote, clamped))
                .build()
        }
    }

    override suspend fun setLowestNote(midi: Int) {
        update { stored ->
            val visible = stored.toDomain().visibleWhiteKeys
            stored.toBuilder()
                .setLowestNote(KeyboardSettings.clampLowestNote(midi, visible))
                .build()
        }
    }

    override suspend fun setLabelMode(mode: KeyLabelMode) {
        update { it.toBuilder().setLabelMode(mode.toProto()).build() }
    }

    override suspend fun setReverbEnabled(enabled: Boolean) {
        update { it.toBuilder().setReverbState(enabled.toReverbStateProto()).build() }
    }

    override suspend fun setMidiOutputEnabled(enabled: Boolean) {
        update { it.toBuilder().setMidiOutputEnabled(enabled).build() }
    }

    override suspend fun setTouchHitTestMode(mode: TouchHitTestMode) {
        update { it.toBuilder().setTouchHitTestMode(mode.toProto()).build() }
    }

    override suspend fun setAreaOverlapThresholdPercent(percent: Int) {
        val clamped = percent.coerceIn(KeyboardSettings.AREA_OVERLAP_THRESHOLD_PERCENT_RANGE)
        update { it.toBuilder().setAreaOverlapThresholdPercent(clamped).build() }
    }

    override suspend fun getLanguageTag(): String? =
        try {
            dataStore.data.first().languageTag.ifEmpty { null }
        } catch (e: IOException) {
            Log.w(TAG, "Reading language failed", e)
            null
        }

    override suspend fun setLanguageTag(tag: String?) {
        update { it.toBuilder().setLanguageTag(tag.orEmpty()).build() }
    }

    private suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        try {
            dataStore.updateData { transform(it) }
        } catch (e: IOException) {
            Log.w(TAG, "Writing user preferences failed", e)
        }
    }
}

private const val TAG = "UserPreferencesRepo"
private const val INITIAL_RETRY_DELAY_MILLIS = 500L
private const val MAX_RETRY_DELAY_MILLIS = 30_000L
private const val MAX_BACKOFF_SHIFT = 6

private fun retryDelayMillis(attempt: Long): Long =
    (INITIAL_RETRY_DELAY_MILLIS shl attempt.coerceAtMost(MAX_BACKOFF_SHIFT.toLong()).toInt())
        .coerceAtMost(MAX_RETRY_DELAY_MILLIS)

private fun UserPreferences.toDomain(): KeyboardSettings {
    val defaults = KeyboardSettings.DEFAULT
    val visible = visibleWhiteKeys.takeIf { it != 0 }?.coerceIn(KeyboardSettings.VISIBLE_WHITE_KEYS_RANGE)
        ?: defaults.visibleWhiteKeys
    val lowest = lowestNote.takeIf { it != 0 } ?: defaults.lowestNote
    return KeyboardSettings(
        visibleWhiteKeys = visible,
        lowestNote = KeyboardSettings.clampLowestNote(lowest, visible),
        labelMode = labelMode.toDomain() ?: defaults.labelMode,
        reverbEnabled = reverbState.toDomain() ?: defaults.reverbEnabled,
        midiOutputEnabled = midiOutputEnabled,
        touchHitTestMode = touchHitTestMode.toDomain() ?: defaults.touchHitTestMode,
        areaOverlapThresholdPercent = areaOverlapThresholdPercent.takeIf { it != 0 }
            ?.coerceIn(KeyboardSettings.AREA_OVERLAP_THRESHOLD_PERCENT_RANGE)
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
