package dev.thomas_kiljanczyk.openpiano.core.testing

import dev.thomas_kiljanczyk.openpiano.core.midi.MidiOutputPort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface MidiOutputEvent {
    data class NoteOn(
        val note: Int,
        val velocity: Int,
    ) : MidiOutputEvent

    data class NoteOff(val note: Int) : MidiOutputEvent
}

class FakeMidiOutputPort : MidiOutputPort {
    private val connected = MutableStateFlow(false)
    private val recorded = mutableListOf<MidiOutputEvent>()

    override val isConnected: StateFlow<Boolean> = connected.asStateFlow()

    val events: List<MidiOutputEvent> get() = synchronized(recorded) { recorded.toList() }

    fun setConnected(value: Boolean) {
        connected.value = value
    }

    fun clear() = synchronized(recorded) { recorded.clear() }

    override fun noteOn(note: Int, velocity: Int) {
        synchronized(recorded) { recorded.add(MidiOutputEvent.NoteOn(note, velocity)) }
    }

    override fun noteOff(note: Int) {
        synchronized(recorded) { recorded.add(MidiOutputEvent.NoteOff(note)) }
    }
}
