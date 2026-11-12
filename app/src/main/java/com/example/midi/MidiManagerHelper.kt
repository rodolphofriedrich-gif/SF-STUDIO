package com.example.midi

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import com.example.audio.AudioEngine
import com.example.sequencer.StepSequencer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Android USB MIDI and Bluetooth LE MIDI controller integration.
 */
class MidiManagerHelper(
    private val context: Context,
    private val audioEngine: AudioEngine,
    private val sequencer: StepSequencer
) {

    // Standard BLE MIDI Service UUID
    private val BLE_MIDI_SERVICE_UUID = UUID.fromString("03B80E5A-EDE8-4B33-A028-51AD18773440")

    private val midiManager: MidiManager? =
        context.getSystemService(Context.MIDI_SERVICE) as? MidiManager

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    data class ConnectedMidiDevice(
        val id: String,
        val name: String,
        val isBluetooth: Boolean,
        val inputPortCount: Int,
        val outputPortCount: Int
    )

    data class DiscoveredBleDevice(
        val device: BluetoothDevice,
        val name: String,
        val address: String,
        val rssi: Int
    )

    private val _connectedDevices = MutableStateFlow<List<ConnectedMidiDevice>>(emptyList())
    val connectedDevices: StateFlow<List<ConnectedMidiDevice>> = _connectedDevices.asStateFlow()

    private val _discoveredBleDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val discoveredBleDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredBleDevices.asStateFlow()

    private val _isBleScanning = MutableStateFlow(false)
    val isBleScanning: StateFlow<Boolean> = _isBleScanning.asStateFlow()

    private val _lastMidiEvent = MutableStateFlow("Nenhum evento MIDI recebido")
    val lastMidiEvent: StateFlow<String> = _lastMidiEvent.asStateFlow()

    private val openDevices = mutableListOf<MidiDevice>()
    private val openOutputPorts = mutableListOf<MidiOutputPort>()

    private val mainHandler = Handler(Looper.getMainLooper())

    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            refreshDevices()
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            refreshDevices()
        }
    }

    private var bleScanCallback: ScanCallback? = null

    init {
        midiManager?.registerDeviceCallback(deviceCallback, mainHandler)
        refreshDevices()
    }

    fun refreshDevices() {
        val manager = midiManager ?: return
        val deviceInfos = manager.devices
        val list = mutableListOf<ConnectedMidiDevice>()

        for (info in deviceInfos) {
            val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
                ?: "Dispositivo MIDI ${info.id}"
            val isBt = info.type == MidiDeviceInfo.TYPE_BLUETOOTH
            list.add(
                ConnectedMidiDevice(
                    id = info.id.toString(),
                    name = name,
                    isBluetooth = isBt,
                    inputPortCount = info.inputPortCount,
                    outputPortCount = info.outputPortCount
                )
            )
        }
        _connectedDevices.value = list

        // Automatically connect to newly available USB MIDI interfaces
        for (info in deviceInfos) {
            if (info.outputPortCount > 0 && openDevices.none { it.info.id == info.id }) {
                connectToDevice(info)
            }
        }
    }

    private fun connectToDevice(info: MidiDeviceInfo) {
        midiManager?.openDevice(info, { device ->
            if (device != null) {
                openDevices.add(device)
                val port = device.openOutputPort(0)
                if (port != null) {
                    openOutputPorts.add(port)
                    port.connect(object : MidiReceiver() {
                        override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
                            parseMidiPacket(msg, offset, count)
                        }
                    })
                }
            }
        }, mainHandler)
    }

    /**
     * Start scanning for Bluetooth LE MIDI keyboards and controllers.
     */
    @SuppressLint("MissingPermission")
    fun startBleMidiScan() {
        val adapter = bluetoothManager?.adapter ?: return
        if (!adapter.isEnabled) return

        val scanner = adapter.bluetoothLeScanner ?: return
        _discoveredBleDevices.value = emptyList()
        _isBleScanning.value = true

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BLE_MIDI_SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bleScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val dev = result.device
                val devName = dev.name ?: "Dispositivo BLE MIDI"
                val current = _discoveredBleDevices.value.toMutableList()
                if (current.none { it.address == dev.address }) {
                    current.add(
                        DiscoveredBleDevice(
                            device = dev,
                            name = devName,
                            address = dev.address,
                            rssi = result.rssi
                        )
                    )
                    _discoveredBleDevices.value = current
                }
            }

            override fun onScanFailed(errorCode: Int) {
                _isBleScanning.value = false
            }
        }

        try {
            scanner.startScan(listOf(filter), settings, bleScanCallback)
            // Stop scanning after 12 seconds to preserve battery
            mainHandler.postDelayed({ stopBleMidiScan() }, 12000L)
        } catch (_: Exception) {
            _isBleScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBleMidiScan() {
        try {
            bluetoothManager?.adapter?.bluetoothLeScanner?.stopScan(bleScanCallback)
        } catch (_: Exception) {}
        _isBleScanning.value = false
        bleScanCallback = null
    }

    /**
     * Connect to a discovered Bluetooth LE MIDI device using Android MidiManager.
     */
    @SuppressLint("MissingPermission")
    fun connectBleDevice(device: BluetoothDevice) {
        stopBleMidiScan()
        midiManager?.openBluetoothDevice(device, { midiDevice ->
            if (midiDevice != null) {
                openDevices.add(midiDevice)
                val port = midiDevice.openOutputPort(0)
                if (port != null) {
                    openOutputPorts.add(port)
                    port.connect(object : MidiReceiver() {
                        override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
                            parseMidiPacket(msg, offset, count)
                        }
                    })
                }
                refreshDevices()
            }
        }, mainHandler)
    }

    /**
     * Parse raw incoming MIDI bytes (Note On, Note Off, CC, Pitch Bend).
     */
    fun parseMidiPacket(msg: ByteArray, offset: Int, count: Int) {
        var i = offset
        while (i < offset + count) {
            val statusByte = msg[i].toInt() and 0xFF
            // Skip real-time clock messages
            if (statusByte >= 0xF8) {
                i++
                continue
            }

            val command = statusByte and 0xF0
            val channel = (statusByte and 0x0F) + 1 // 1 to 16

            when (command) {
                0x90 -> { // Note On
                    if (i + 2 < offset + count) {
                        val note = msg[i + 1].toInt() and 0x7F
                        val velocity = msg[i + 2].toInt() and 0x7F
                        if (velocity > 0) {
                            dispatchNoteOn(channel, note, velocity / 127.0f)
                            _lastMidiEvent.value = "Note ON: Ch $channel, Nota $note, Vel $velocity"
                        } else {
                            dispatchNoteOff(channel, note)
                            _lastMidiEvent.value = "Note OFF: Ch $channel, Nota $note"
                        }
                        i += 3
                    } else i++
                }
                0x80 -> { // Note Off
                    if (i + 2 < offset + count) {
                        val note = msg[i + 1].toInt() and 0x7F
                        dispatchNoteOff(channel, note)
                        _lastMidiEvent.value = "Note OFF: Ch $channel, Nota $note"
                        i += 3
                    } else i++
                }
                0xB0 -> { // Control Change (Remote Control & Automation)
                    if (i + 2 < offset + count) {
                        val ccNumber = msg[i + 1].toInt() and 0x7F
                        val ccValue = msg[i + 2].toInt() and 0x7F
                        handleControlChange(channel, ccNumber, ccValue)
                        _lastMidiEvent.value = "CC $ccNumber: Ch $channel, Valor $ccValue"
                        i += 3
                    } else i++
                }
                0xE0 -> { // Pitch Bend
                    if (i + 2 < offset + count) {
                        val lsb = msg[i + 1].toInt() and 0x7F
                        val msb = msg[i + 2].toInt() and 0x7F
                        val bend = ((msb shl 7) or lsb) - 8192
                        handlePitchBend(channel, bend)
                        i += 3
                    } else i++
                }
                else -> i++
            }
        }
    }

    private fun dispatchNoteOn(midiChannel: Int, note: Int, velocity: Float) {
        val tracks = audioEngine.tracksFlow.value
        for (track in tracks) {
            // Check if track matches MIDI channel or is set to Omni (0)
            if (track.midiChannel == 0 || track.midiChannel == midiChannel) {
                audioEngine.noteOn(track.id, note, velocity)
            }
        }
    }

    private fun dispatchNoteOff(midiChannel: Int, note: Int) {
        val tracks = audioEngine.tracksFlow.value
        for (track in tracks) {
            if (track.midiChannel == 0 || track.midiChannel == midiChannel) {
                audioEngine.noteOff(track.id, note)
            }
        }
    }

    private fun handleControlChange(channel: Int, cc: Int, value: Int) {
        // Transport remote control
        when (cc) {
            114 -> { // Remote Play/Pause toggle
                if (sequencer.isPlaying.value) sequencer.pause() else sequencer.play()
                return
            }
            115 -> { // Remote Tap Tempo
                sequencer.tapTempo()
                return
            }
        }

        // Channel parameter control
        val tracks = audioEngine.tracksFlow.value
        for (track in tracks) {
            if (track.midiChannel == 0 || track.midiChannel == channel) {
                when (cc) {
                    7 -> { // Volume
                        val newVol = (value / 127.0f) * 1.5f
                        audioEngine.updateTrack(track.id) { it.copy(volume = newVol) }
                    }
                    10 -> { // Pan
                        val newPan = (value / 63.5f) - 1.0f
                        audioEngine.updateTrack(track.id) { it.copy(pan = newPan) }
                    }
                    74 -> { // Cutoff Filter
                        val freq = 40f + (value / 127.0f).let { it * it } * 18000f
                        audioEngine.updateTrack(track.id) {
                            it.copy(filter = it.filter.copy(cutoffHz = freq))
                        }
                    }
                    71 -> { // Resonance
                        val res = 0.2f + (value / 127.0f) * 5.0f
                        audioEngine.updateTrack(track.id) {
                            it.copy(filter = it.filter.copy(resonance = res))
                        }
                    }
                }
            }
        }
    }

    private fun handlePitchBend(channel: Int, bend: Int) {
        val cents = (bend / 8192.0f) * 200.0f // +/- 200 cents (2 semitones)
        val tracks = audioEngine.tracksFlow.value
        for (track in tracks) {
            if (track.midiChannel == 0 || track.midiChannel == channel) {
                audioEngine.updateTrack(track.id) { it.copy(fineTuneCents = cents) }
            }
        }
    }

    fun release() {
        stopBleMidiScan()
        try {
            midiManager?.unregisterDeviceCallback(deviceCallback)
            openOutputPorts.forEach { it.close() }
            openDevices.forEach { it.close() }
        } catch (_: Exception) {}
        openOutputPorts.clear()
        openDevices.clear()
    }
}
