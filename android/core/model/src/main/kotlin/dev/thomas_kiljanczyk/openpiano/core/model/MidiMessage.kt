package dev.thomas_kiljanczyk.openpiano.core.model

private const val STATUS_MASK = 0xF0
private const val CHANNEL_MASK = 0x0F
private const val DATA_MASK = 0x7F
private const val STATUS_SHIFT = 16
private const val DATA1_SHIFT = 8

object MidiMessage {
    const val NOTE_OFF = 0x80
    const val NOTE_ON = 0x90

    fun pack(status: Int, channel: Int, data1: Int, data2: Int): Int {
        val statusByte = (status and STATUS_MASK) or (channel and CHANNEL_MASK)
        return (statusByte shl STATUS_SHIFT) or
            ((data1 and DATA_MASK) shl DATA1_SHIFT) or
            (data2 and DATA_MASK)
    }

    fun status(packed: Int): Int = (packed shr STATUS_SHIFT) and STATUS_MASK

    fun channel(packed: Int): Int = (packed shr STATUS_SHIFT) and CHANNEL_MASK

    fun data1(packed: Int): Int = (packed shr DATA1_SHIFT) and DATA_MASK

    fun data2(packed: Int): Int = packed and DATA_MASK

    fun toBytes(status: Int, channel: Int, data1: Int, data2: Int): ByteArray {
        val statusByte = (status and STATUS_MASK) or (channel and CHANNEL_MASK)
        return byteArrayOf(
            statusByte.toByte(),
            (data1 and DATA_MASK).toByte(),
            (data2 and DATA_MASK).toByte(),
        )
    }
}
