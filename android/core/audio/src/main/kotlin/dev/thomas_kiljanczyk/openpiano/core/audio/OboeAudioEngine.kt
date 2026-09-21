package dev.thomas_kiljanczyk.openpiano.core.audio

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.thomas_kiljanczyk.openpiano.core.common.allowingThreadDiskReads
import dev.thomas_kiljanczyk.openpiano.core.common.di.ApplicationScope
import dev.thomas_kiljanczyk.openpiano.core.common.di.Dispatcher
import dev.thomas_kiljanczyk.openpiano.core.common.di.OpenPianoDispatcher
import dev.thomas_kiljanczyk.openpiano.core.model.MidiMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OpenPianoAudio"
private const val SOUND_FONT_ASSET = "piano.sf3"
private const val CHANNEL = 0

@Singleton
class OboeAudioEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val scope: CoroutineScope,
    @param:Dispatcher(OpenPianoDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : AudioEngine {

    private val handle: Long = nativeCreate()
    private val lifecycleLock = Mutex()
    private val ready = MutableStateFlow(false)

    override val isReady: StateFlow<Boolean> = ready.asStateFlow()

    override fun start() {
        scope.launch {
            lifecycleLock.withLock {
                if (!ready.value && !loadSoundFont()) return@withLock
                if (!nativeStart(handle)) {
                    Log.e(TAG, "audio stream failed to start")
                    return@withLock
                }
                Log.i(TAG, stats().toString())
            }
        }
    }

    override fun stop() {
        scope.launch { lifecycleLock.withLock { nativeStop(handle) } }
    }

    override fun noteOn(note: Int, velocity: Int) =
        nativeSend(handle, MidiMessage.pack(MidiMessage.NOTE_ON, CHANNEL, note, velocity))

    override fun noteOff(note: Int) =
        nativeSend(handle, MidiMessage.pack(MidiMessage.NOTE_OFF, CHANNEL, note, 0))

    override fun setReverbEnabled(enabled: Boolean) = nativeSetReverbEnabled(handle, enabled)

    fun stats(): AudioEngineStats {
        val raw = nativeStats(handle)
        return AudioEngineStats(
            sampleRate = raw[0],
            framesPerBurst = raw[1],
            exclusive = raw[2] != 0,
            xRunCount = raw[3],
            droppedEvents = raw[4],
        )
    }

    override fun release() {
        nativeStop(handle)
        nativeDestroy(handle)
    }

    private suspend fun loadSoundFont(): Boolean = withContext(ioDispatcher) {
        val bytes = context.assets.open(SOUND_FONT_ASSET).use { it.readBytes() }
        val loaded = nativeLoadSoundFont(handle, bytes)
        if (!loaded) Log.e(TAG, "failed to decode $SOUND_FONT_ASSET")
        ready.value = loaded
        loaded
    }

    private external fun nativeCreate(): Long

    private external fun nativeDestroy(handle: Long)

    private external fun nativeLoadSoundFont(handle: Long, data: ByteArray): Boolean

    private external fun nativeStart(handle: Long): Boolean

    private external fun nativeStop(handle: Long)

    private external fun nativeSend(handle: Long, packed: Int)

    private external fun nativeSetReverbEnabled(handle: Long, enabled: Boolean)

    private external fun nativeStats(handle: Long): IntArray

    private companion object {
        init {
            // Loaded synchronously on injection; classloader resolution always touches disk.
            allowingThreadDiskReads {
                System.loadLibrary("openpiano_audio")
            }
        }
    }
}
