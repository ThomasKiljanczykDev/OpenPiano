package dev.thomas_kiljanczyk.openpiano.core.audio

import kotlinx.coroutines.flow.StateFlow

interface AudioEngine {
    /** False until the soundfont has been decoded; the keyboard is silent while it is false. */
    val isReady: StateFlow<Boolean>

    fun start()

    fun stop()

    fun noteOn(note: Int, velocity: Int)

    fun noteOff(note: Int)

    fun setReverbEnabled(enabled: Boolean)

    fun release()
}

const val DEFAULT_VELOCITY = 100
