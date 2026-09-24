package dev.thomas_kiljanczyk.openpiano.core.midi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.DeprecatedSinceApi
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.thomas_kiljanczyk.openpiano.core.model.MidiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val OUTPUT_CHANNEL = 0
private const val USB_PERIPHERAL_INPUT_PORT_INDEX = 0
private const val CONTROL_CHANGE = 0xB0
private const val CC_ALL_SOUND_OFF = 120
private const val CC_ALL_NOTES_OFF = 123

internal class MidiSender {
    @Volatile
    var receiver: MidiReceiver? = null

    fun send(status: Int, channel: Int, data1: Int, data2: Int) {
        val bytes = MidiMessage.toBytes(status, channel, data1, data2)
        // onSend throws if the port went stale between the last status update and this send.
        runCatching { receiver?.onSend(bytes, 0, bytes.size, System.nanoTime()) }
    }
}

// Device state is only touched on the main looper, where every MidiManager callback is delivered.
@Singleton
class UsbMidiOutputPort @Inject constructor(@ApplicationContext context: Context) : MidiOutputPort {

    private val handler = Handler(Looper.getMainLooper())
    private val midiManager = context.getSystemService(MidiManager::class.java)
    private val sender = MidiSender()

    private var connectedDevice: MidiDevice? = null
    private var inputPort: MidiInputPort? = null

    private val mutableIsConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = mutableIsConnected.asStateFlow()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            midiManager?.registerDeviceCallback(
                MidiManager.TRANSPORT_MIDI_BYTE_STREAM,
                context.mainExecutor,
                UsbDeviceCallback(),
            )
        } else {
            registerDeviceCallbackLegacy()
        }
        openFirstUsbDevice()
    }

    // Pre-33 there is no UMP transport, so the legacy callback only ever reports byte-stream devices.
    @DeprecatedSinceApi(api = Build.VERSION_CODES.TIRAMISU)
    private fun registerDeviceCallbackLegacy() {
        @Suppress("DEPRECATION")
        midiManager?.registerDeviceCallback(UsbDeviceCallback(), handler)
    }

    // onDeviceAdded only fires for devices attached after registration.
    private inner class UsbDeviceCallback : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            if (isUsbOutputTarget(device)) openFirstOf(listOf(device))
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            if (device.id != connectedDevice?.info?.id) return
            disconnect()
            openFirstUsbDevice()
        }
    }

    private fun isUsbOutputTarget(info: MidiDeviceInfo) =
        info.type == MidiDeviceInfo.TYPE_USB && info.inputPortCount > 0

    private fun openFirstUsbDevice() {
        val devices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            midiManager?.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM)
        } else {
            getDevicesLegacy()
        }
        openFirstOf(devices?.filter(::isUsbOutputTarget).orEmpty())
    }

    @Suppress("DEPRECATION")
    @DeprecatedSinceApi(api = Build.VERSION_CODES.TIRAMISU)
    private fun getDevicesLegacy(): List<MidiDeviceInfo>? = midiManager?.devices?.toList()

    private fun openFirstOf(candidates: List<MidiDeviceInfo>) {
        val info = candidates.firstOrNull()
        if (connectedDevice != null || info == null) return
        midiManager?.openDevice(
            info,
            { opened -> if (!attach(opened)) openFirstOf(candidates.drop(1)) },
            handler,
        )
    }

    /** False when [opened] could not be used, so the caller moves on to the next candidate. */
    private fun attach(opened: MidiDevice?): Boolean {
        if (opened == null) return false
        val port = if (connectedDevice == null) {
            runCatching { opened.openInputPort(USB_PERIPHERAL_INPUT_PORT_INDEX) }.getOrNull()
        } else {
            null
        }
        if (port == null) {
            runCatching { opened.close() }
        } else {
            connectedDevice = opened
            inputPort = port
            sender.receiver = port
            mutableIsConnected.value = true
        }
        return connectedDevice != null
    }

    private fun disconnect() {
        sender.receiver = null
        mutableIsConnected.value = false
        inputPort?.let { runCatching { it.close() } }
        connectedDevice?.let { runCatching { it.close() } }
        inputPort = null
        connectedDevice = null
    }

    override fun noteOn(note: Int, velocity: Int) {
        sender.send(MidiMessage.NOTE_ON, OUTPUT_CHANNEL, note, velocity)
    }

    override fun noteOff(note: Int) {
        sender.send(MidiMessage.NOTE_OFF, OUTPUT_CHANNEL, note, data2 = 0)
    }

    override fun allNotesOff() {
        sender.send(CONTROL_CHANGE, OUTPUT_CHANNEL, CC_ALL_NOTES_OFF, data2 = 0)
        sender.send(CONTROL_CHANGE, OUTPUT_CHANNEL, CC_ALL_SOUND_OFF, data2 = 0)
    }
}
