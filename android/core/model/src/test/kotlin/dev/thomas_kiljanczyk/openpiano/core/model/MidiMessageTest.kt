package dev.thomas_kiljanczyk.openpiano.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MidiMessageTest {

    @Test
    fun `note on round trips`() {
        val packed = MidiMessage.pack(MidiMessage.NOTE_ON, channel = 0, data1 = 60, data2 = 100)
        assertEquals(MidiMessage.NOTE_ON, MidiMessage.status(packed))
        assertEquals(0, MidiMessage.channel(packed))
        assertEquals(60, MidiMessage.data1(packed))
        assertEquals(100, MidiMessage.data2(packed))
    }

    @Test
    fun `channel occupies the low nibble of the status byte`() {
        val packed = MidiMessage.pack(MidiMessage.NOTE_OFF, channel = 9, data1 = 127, data2 = 0)
        assertEquals(0x89_7F_00, packed)
        assertEquals(9, MidiMessage.channel(packed))
    }

    @Test
    fun `data bytes are masked to seven bits`() {
        val packed = MidiMessage.pack(MidiMessage.NOTE_ON, channel = 0, data1 = 0xFF, data2 = 0xFF)
        assertEquals(0x7F, MidiMessage.data1(packed))
        assertEquals(0x7F, MidiMessage.data2(packed))
    }

    @Test
    fun `toBytes produces a three byte MIDI 1_0 message`() {
        val bytes = MidiMessage.toBytes(MidiMessage.NOTE_ON, channel = 0, data1 = 60, data2 = 100)
        assertEquals(3, bytes.size)
        assertEquals(0x90.toByte(), bytes[0])
        assertEquals(60.toByte(), bytes[1])
        assertEquals(100.toByte(), bytes[2])
    }

    @Test
    fun `toBytes encodes channel in the low nibble`() {
        val bytes = MidiMessage.toBytes(MidiMessage.NOTE_OFF, channel = 9, data1 = 127, data2 = 0)
        assertEquals(0x89.toByte(), bytes[0])
    }

    @Test
    fun `toBytes masks data bytes to seven bits`() {
        val bytes = MidiMessage.toBytes(MidiMessage.NOTE_ON, channel = 0, data1 = 0xFF, data2 = 0xFF)
        assertEquals(0x7F.toByte(), bytes[1])
        assertEquals(0x7F.toByte(), bytes[2])
    }
}
