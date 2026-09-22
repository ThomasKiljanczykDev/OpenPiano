package dev.thomas_kiljanczyk.openpiano.core.model

/** The 88-key acoustic piano range, A0 to C8, addressed by white-key index. */
object Piano {
    const val LOWEST_MIDI = 21
    const val HIGHEST_MIDI = 108
    const val WHITE_KEYS_PER_OCTAVE = 7

    private val whiteKeys: IntArray =
        (LOWEST_MIDI..HIGHEST_MIDI).filterNot(Note::isBlack).toIntArray()

    val whiteKeyCount: Int = whiteKeys.size

    fun whiteKeyAt(index: Int): Int = whiteKeys[index.coerceIn(0, whiteKeyCount - 1)]

    /** Index of [midi] when it is a white key, otherwise of the white key below it. */
    fun whiteKeyIndexOf(midi: Int): Int {
        val clamped = midi.coerceIn(LOWEST_MIDI, HIGHEST_MIDI)
        val above = whiteKeys.indexOfFirst { it > clamped }
        return (if (above < 0) whiteKeyCount else above) - 1
    }
}
