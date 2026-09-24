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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OpenPianoAudio"
private const val SOUND_FONT_ASSET = "piano.sf3"
private const val CHANNEL = 0
private const val CONTROL_CHANGE = 0xB0
private const val CC_ALL_NOTES_OFF = 123
private const val START_ATTEMPTS = 4
private const val START_RETRY_BASE_MS = 250L

@Singleton
class OboeAudioEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val scope: CoroutineScope,
    @param:Dispatcher(OpenPianoDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : AudioEngine {

    // Never freed: noteOn/noteOff read it lock-free from the UI thread; the process owns it.
    private val handle: Long = nativeCreate()

    // Serial, in call order: an earlier stop() must never overtake a later start().
    private val lifecycleDispatcher = ioDispatcher.limitedParallelism(1)
    private val ready = MutableStateFlow(false)
    private var startJob: Job? = null

    override val isReady: StateFlow<Boolean> = ready.asStateFlow()

    @Synchronized
    override fun start() {
        startJob?.cancel()
        startJob = scope.launch(lifecycleDispatcher) {
            if (!ready.value && !loadSoundFont()) return@launch
            startStreamWithRetry()
        }
    }

    // Cancelling the start job ends a pending retry at its next delay, before this stop runs.
    @Synchronized
    override fun stop() {
        startJob?.cancel()
        startJob = null
        scope.launch(lifecycleDispatcher) { nativeStop(handle) }
    }

    private suspend fun startStreamWithRetry() {
        for (attempt in 0 until START_ATTEMPTS) {
            if (nativeStart(handle)) {
                Log.i(TAG, stats().toString())
                return
            }
            Log.e(TAG, "audio stream failed to start, attempt ${attempt + 1}")
            if (attempt < START_ATTEMPTS - 1) delay(START_RETRY_BASE_MS shl attempt)
        }
    }

    override fun noteOn(note: Int, velocity: Int) =
        nativeSend(handle, MidiMessage.pack(MidiMessage.NOTE_ON, CHANNEL, note, velocity))

    override fun noteOff(note: Int) =
        nativeSend(handle, MidiMessage.pack(MidiMessage.NOTE_OFF, CHANNEL, note, 0))

    override fun allNotesOff() =
        nativeSend(handle, MidiMessage.pack(CONTROL_CHANGE, CHANNEL, CC_ALL_NOTES_OFF, 0))

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

    private fun loadSoundFont(): Boolean {
        val loaded = try {
            val bytes = context.assets.open(SOUND_FONT_ASSET).use { it.readBytes() }
            nativeLoadSoundFont(handle, bytes).also { if (!it) Log.e(TAG, "failed to decode $SOUND_FONT_ASSET") }
        } catch (e: IOException) {
            Log.e(TAG, "failed to read $SOUND_FONT_ASSET", e)
            false
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "out of memory loading $SOUND_FONT_ASSET", e)
            false
        }
        ready.value = loaded
        return loaded
    }

    private external fun nativeCreate(): Long

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
