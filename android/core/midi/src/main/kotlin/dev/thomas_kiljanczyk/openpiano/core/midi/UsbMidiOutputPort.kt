package dev.thomas_kiljanczyk.openpiano.core.midi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.thomas_kiljanczyk.openpiano.core.model.MidiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val OUTPUT_CHANNEL = 0
private const val USB_PERIPHERAL_INPUT_PORT_INDEX = 0

internal class MidiSender {
    var receiver: MidiReceiver? = null

    fun send(status: Int, channel: Int, data1: Int, data2: Int) {
        val bytes = MidiMessage.toBytes(status, channel, data1, data2)
        // onSend throws if the port went stale between the last status update and this send.
        runCatching { receiver?.onSend(bytes, 0, bytes.size, System.nanoTime()) }
    }
}

@Singleton
class UsbMidiOutputPort @Inject constructor(@ApplicationContext context: Context) : MidiOutputPort {

    private val handler = Handler(Looper.getMainLooper())
    private val midiManager = context.getSystemService(MidiManager::class.java)
    private val sender = MidiSender()

    private val mutableIsConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = mutableIsConnected.asStateFlow()

    // onDeviceAdded only fires for devices attached after registration.
    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            if (device.type == MidiDeviceInfo.TYPE_USB) openUsbDevice(device)
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            if (device.type == MidiDeviceInfo.TYPE_USB) {
                sender.receiver = null
                mutableIsConnected.value = false
            }
        }
    }

    init {
        // Executor-based overloads require API 31; minSdk is 26.
        @Suppress("DEPRECATION")
        run {
            midiManager?.registerDeviceCallback(deviceCallback, handler)
            midiManager?.devices?.firstOrNull { it.type == MidiDeviceInfo.TYPE_USB }?.let(::openUsbDevice)
        }
    }

    private fun openUsbDevice(info: MidiDeviceInfo) {
        midiManager?.openDevice(info, ::onDeviceOpened, handler)
    }

    private fun onDeviceOpened(device: MidiDevice?) {
        val port = device?.openInputPort(USB_PERIPHERAL_INPUT_PORT_INDEX)
        sender.receiver = port
        mutableIsConnected.value = port != null
    }

    override fun noteOn(note: Int, velocity: Int) {
        sender.send(MidiMessage.NOTE_ON, OUTPUT_CHANNEL, note, velocity)
    }

    override fun noteOff(note: Int) {
        sender.send(MidiMessage.NOTE_OFF, OUTPUT_CHANNEL, note, data2 = 0)
    }
}
