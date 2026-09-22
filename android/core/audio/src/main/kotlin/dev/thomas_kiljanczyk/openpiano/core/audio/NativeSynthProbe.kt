package dev.thomas_kiljanczyk.openpiano.core.audio

/**
 * Diagnostic offline render: loads a soundfont into the native synth with no Oboe stream, holds
 * every given note down and renders one second at 48 kHz, returning the loudest sample. An empty
 * note list renders without any note-on. Returns -1 when the soundfont fails to decode.
 */
object NativeSynthProbe {

    external fun nativePeakAmplitude(soundFont: ByteArray, notes: IntArray, reverbEnabled: Boolean): Float

    /**
     * Holds every given note down for [sustainBursts] 192-frame bursts, releases them, then
     * returns the loudest sample rendered over the following [releaseBursts] bursts — used to
     * check whether a reverb tail outlasts note-off.
     */
    external fun nativeReleasePeakAmplitude(
        soundFont: ByteArray,
        notes: IntArray,
        sustainBursts: Int,
        releaseBursts: Int,
        reverbEnabled: Boolean,
    ): Float

    init {
        System.loadLibrary("openpiano_audio")
    }
}
