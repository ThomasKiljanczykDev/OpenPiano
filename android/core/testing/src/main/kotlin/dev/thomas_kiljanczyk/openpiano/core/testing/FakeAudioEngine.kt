package dev.thomas_kiljanczyk.openpiano.core.testing

import dev.thomas_kiljanczyk.openpiano.core.audio.AudioEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AudioEvent {
    data class NoteOn(
        val note: Int,
        val velocity: Int,
    ) : AudioEvent

    data class NoteOff(val note: Int) : AudioEvent

    data object AllNotesOff : AudioEvent
}

class FakeAudioEngine : AudioEngine {
    private val ready = MutableStateFlow(true)
    private val recorded = mutableListOf<AudioEvent>()

    override val isReady: StateFlow<Boolean> = ready.asStateFlow()

    val events: List<AudioEvent> get() = synchronized(recorded) { recorded.toList() }

    var started: Boolean = false
        private set

    var reverbEnabled: Boolean = true
        private set

    fun setReady(value: Boolean) {
        ready.value = value
    }

    fun clear() = synchronized(recorded) { recorded.clear() }

    override fun start() {
        started = true
    }

    override fun stop() {
        started = false
    }

    override fun noteOn(note: Int, velocity: Int) {
        synchronized(recorded) { recorded.add(AudioEvent.NoteOn(note, velocity)) }
    }

    override fun noteOff(note: Int) {
        synchronized(recorded) { recorded.add(AudioEvent.NoteOff(note)) }
    }

    override fun allNotesOff() {
        synchronized(recorded) { recorded.add(AudioEvent.AllNotesOff) }
    }

    override fun setReverbEnabled(enabled: Boolean) {
        reverbEnabled = enabled
    }
}
