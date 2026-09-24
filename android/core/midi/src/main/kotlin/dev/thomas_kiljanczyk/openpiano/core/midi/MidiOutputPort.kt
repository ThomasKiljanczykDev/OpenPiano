package dev.thomas_kiljanczyk.openpiano.core.midi

import kotlinx.coroutines.flow.StateFlow

interface MidiOutputPort {
    val isConnected: StateFlow<Boolean>

    fun noteOn(note: Int, velocity: Int)

    fun noteOff(note: Int)

    fun allNotesOff()
}
