package dev.thomas_kiljanczyk.openpiano.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PianoTest {

    @Test
    fun `spans the 52 white keys of an 88 key piano`() {
        assertEquals(52, Piano.whiteKeyCount)
        assertEquals(Piano.LOWEST_MIDI, Piano.whiteKeyAt(0))
        assertEquals(Piano.HIGHEST_MIDI, Piano.whiteKeyAt(Piano.whiteKeyCount - 1))
    }

    @Test
    fun `white key index round trips`() {
        for (index in 0 until Piano.whiteKeyCount) {
            assertEquals(index, Piano.whiteKeyIndexOf(Piano.whiteKeyAt(index)))
        }
    }

    @Test
    fun `black keys resolve to the white key below`() {
        assertEquals(Piano.whiteKeyIndexOf(60), Piano.whiteKeyIndexOf(61))
    }

    @Test
    fun `an octave is seven white keys`() {
        val c4 = Piano.whiteKeyIndexOf(60)
        assertEquals(72, Piano.whiteKeyAt(c4 + Piano.WHITE_KEYS_PER_OCTAVE))
    }

    @Test
    fun `indices clamp to the piano range`() {
        assertEquals(Piano.LOWEST_MIDI, Piano.whiteKeyAt(-10))
        assertEquals(Piano.HIGHEST_MIDI, Piano.whiteKeyAt(999))
        assertEquals(0, Piano.whiteKeyIndexOf(0))
        assertEquals(Piano.whiteKeyCount - 1, Piano.whiteKeyIndexOf(127))
    }
}
