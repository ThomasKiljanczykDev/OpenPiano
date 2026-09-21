package dev.thomas_kiljanczyk.openpiano.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private const val WIDTH = 700f
private const val HEIGHT = 200f

class KeyboardLayoutTest {

    private fun layout(lowest: Int = 60, whiteKeys: Int = 7) =
        KeyboardLayout(lowest, whiteKeys, WIDTH, HEIGHT)

    @Test
    fun `white keys tile the full width`() {
        val keys = layout().whiteKeys
        assertEquals(7, keys.size)
        assertEquals(0f, keys.first().left)
        assertEquals(WIDTH, keys.last().right)
        assertEquals(100f, keys[0].width)
    }

    @Test
    fun `white keys skip accidentals`() {
        assertEquals(listOf(60, 62, 64, 65, 67, 69, 71), layout().whiteKeys.map { it.midiNote })
    }

    @Test
    fun `an octave has five black keys centred on white boundaries`() {
        val black = layout().blackKeys
        assertEquals(listOf(61, 63, 66, 68, 70), black.map { it.midiNote })
        assertEquals(100f, black[0].left + black[0].width / 2f)
        assertEquals(60f, black[0].width)
        assertEquals(124f, black[0].height)
    }

    @Test
    fun `no black key sits between E-F or B-C`() {
        val boundaries = layout().blackKeys.map { it.left + it.width / 2f }
        assertEquals(listOf(100f, 200f, 400f, 500f, 600f), boundaries)
    }

    @Test
    fun `hit test prefers black keys in the overlap region`() {
        val l = layout()
        assertEquals(61, l.noteAt(100f, 10f))
        assertEquals(60, l.noteAt(99f, HEIGHT - 1f))
        assertEquals(62, l.noteAt(140f, 10f))
    }

    @Test
    fun `hit test returns null outside the keyboard`() {
        val l = layout()
        assertNull(l.noteAt(-1f, 10f))
        assertNull(l.noteAt(WIDTH, 10f))
        assertNull(l.noteAt(10f, HEIGHT))
    }

    @Test
    fun `layout spanning more than one octave keeps ascending notes`() {
        val l = layout(whiteKeys = 15)
        assertEquals(60, l.whiteKeys.first().midiNote)
        assertEquals(84, l.highestNote)
    }

    @Test
    fun `lowest note must be white`() {
        assertFailsWith<IllegalArgumentException> { layout(lowest = 61) }
    }

    @Test
    fun `area hit test with a small touch returns the single covered key`() {
        val l = layout()
        val notes = l.notesInArea(x = 50f, y = 150f, touchMajor = 20f, touchMinor = 20f)
        assertEquals(setOf(60), notes)
    }

    @Test
    fun `area hit test below the black keys can chord two adjacent white keys`() {
        val l = layout()
        val notes = l.notesInArea(x = 100f, y = 150f, touchMajor = 140f, touchMinor = 20f)
        assertEquals(setOf(60, 62), notes)
    }

    @Test
    fun `area hit test on a black key excludes its own adjacent white keys`() {
        val l = layout()
        val notes = l.notesInArea(x = 100f, y = 10f, touchMajor = 20f, touchMinor = 20f)
        assertEquals(setOf(61), notes)
    }

    @Test
    fun `area hit test lets two qualifying black keys chord, excluding their shared white key`() {
        val l = layout()
        val notes = l.notesInArea(x = 150f, y = 62f, touchMajor = 160f, touchMinor = 124f)
        assertEquals(setOf(61, 63), notes)
    }

    @Test
    fun `area hit test below the overlap threshold returns nothing`() {
        val l = layout()
        val notes = l.notesInArea(x = -31f, y = 100f, touchMajor = 100f, touchMinor = 100f)
        assertEquals(emptySet(), notes)
    }

    @Test
    fun `area hit test honors a custom overlap threshold`() {
        val l = layout()
        val notes = l.notesInArea(
            x = -31f,
            y = 100f,
            touchMajor = 100f,
            touchMinor = 100f,
            overlapThreshold = 0.15f,
        )
        assertEquals(setOf(60), notes)
    }

    @Test
    fun `area hit test at a zero-size touch falls back to the point hit test`() {
        val l = layout()
        assertEquals(setOf(61), l.notesInArea(x = 100f, y = 10f, touchMajor = 0f, touchMinor = 0f))
        assertEquals(emptySet(), l.notesInArea(x = -1f, y = 10f, touchMajor = 0f, touchMinor = 0f))
    }
}
