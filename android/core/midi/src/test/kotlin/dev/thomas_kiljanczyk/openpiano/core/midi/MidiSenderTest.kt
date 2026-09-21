package dev.thomas_kiljanczyk.openpiano.core.midi

import android.media.midi.MidiReceiver
import org.junit.Assert.assertEquals
import org.junit.Test

private class RecordingMidiReceiver : MidiReceiver() {
    val received = mutableListOf<ByteArray>()

    override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
        received.add(msg.copyOfRange(offset, offset + count))
    }
}

class MidiSenderTest {

    @Test
    fun `note-on sends a MIDI 1_0 note-on message to the attached receiver`() {
        val sender = MidiSender()
        val receiver = RecordingMidiReceiver()
        sender.receiver = receiver

        sender.send(0x90, channel = 0, data1 = 60, data2 = 100)

        assertEquals(1, receiver.received.size)
        assertEquals(listOf(0x90.toByte(), 60.toByte(), 100.toByte()), receiver.received[0].toList())
    }

    @Test
    fun `note-off sends a MIDI 1_0 note-off message to the attached receiver`() {
        val sender = MidiSender()
        val receiver = RecordingMidiReceiver()
        sender.receiver = receiver

        sender.send(0x80, channel = 0, data1 = 60, data2 = 0)

        assertEquals(1, receiver.received.size)
        assertEquals(listOf(0x80.toByte(), 60.toByte(), 0.toByte()), receiver.received[0].toList())
    }

    @Test
    fun `sends with no receiver are silently swallowed`() {
        val sender = MidiSender()

        sender.send(0x90, channel = 0, data1 = 60, data2 = 100)
    }
}
