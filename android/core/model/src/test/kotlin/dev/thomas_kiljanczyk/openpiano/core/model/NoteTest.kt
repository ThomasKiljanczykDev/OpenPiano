package dev.thomas_kiljanczyk.openpiano.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NoteTest {

    @Test
    fun `middle C is C4`() {
        assertEquals("C4", Note.fullName(60))
    }

    @Test
    fun `octave numbering follows scientific pitch`() {
        assertEquals(-1, Note.octave(0))
        assertEquals(3, Note.octave(48))
        assertEquals(9, Note.octave(127))
    }

    @Test
    fun `black keys are the five accidentals`() {
        val black = (60..71).filter { Note.isBlack(it) }
        assertEquals(listOf(61, 63, 66, 68, 70), black)
    }

    @Test
    fun `C is never black`() {
        assertFalse(Note.isBlack(0))
        assertFalse(Note.isBlack(60))
    }

    @Test
    fun `firstOfOctave is the C of that octave`() {
        assertEquals(24, Note.firstOfOctave(1))
        assertEquals(60, Note.firstOfOctave(4))
        assertTrue(Note.fullName(Note.firstOfOctave(7)) == "C7")
    }
}
