package dev.thomas_kiljanczyk.openpiano.core.model

object Note {
    const val MIDI_MIN = 0
    const val MIDI_MAX = 127
    const val SEMITONES_PER_OCTAVE = 12

    private val NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val BLACK = booleanArrayOf(
        false, true, false, true, false, false, true, false, true, false, true, false,
    )

    fun pitchClass(midi: Int): Int = midi.mod(SEMITONES_PER_OCTAVE)

    fun octave(midi: Int): Int = Math.floorDiv(midi, SEMITONES_PER_OCTAVE) - 1

    fun isBlack(midi: Int): Boolean = BLACK[pitchClass(midi)]

    fun pitchName(midi: Int): String = NAMES[pitchClass(midi)]

    fun fullName(midi: Int): String = pitchName(midi) + octave(midi)

    fun firstOfOctave(octave: Int): Int = (octave + 1) * SEMITONES_PER_OCTAVE
}
