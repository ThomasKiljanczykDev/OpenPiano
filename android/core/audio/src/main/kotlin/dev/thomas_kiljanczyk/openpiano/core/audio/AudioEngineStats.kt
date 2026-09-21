package dev.thomas_kiljanczyk.openpiano.core.audio

data class AudioEngineStats(
    val sampleRate: Int,
    val framesPerBurst: Int,
    val exclusive: Boolean,
    val xRunCount: Int,
    val droppedEvents: Int,
)
