package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngine
import com.example.midi.MidiExporter
import com.example.midi.MidiManagerHelper
import com.example.sequencer.StepSequencer
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioHighlight
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MidiBleScreen(
    midiHelper: MidiManagerHelper,
    audioEngine: AudioEngine,
    sequencer: StepSequencer,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectedDevices by midiHelper.connectedDevices.collectAsState()
    val discoveredBle by midiHelper.discoveredBleDevices.collectAsState()
    val isScanning by midiHelper.isBleScanning.collectAsState()
    val lastEvent by midiHelper.lastMidiEvent.collectAsState()
    val bpm by sequencer.bpm.collectAsState()

    var exportMessage by remember { mutableStateOf<String?>(null) }

    // Bluetooth permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            midiHelper.startBleMidiScan()
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left Column: USB MIDI & Bluetooth LE Hardware Connections
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. USB MIDI Hardware
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(StudioSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Usb, contentDescription = "USB MIDI", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DISPOSITIVOS USB MIDI (PLUG & PLAY)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    Row(
                        modifier = Modifier
                            .background(StudioCard, RoundedCornerShape(4.dp))
                            .clickable { midiHelper.refreshDevices() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = TextSecondary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("ATUALIZAR", fontSize = 8.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (connectedDevices.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(connectedDevices) { dev ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StudioCard, RoundedCornerShape(4.dp))
                                    .border(1.dp, StudioBorder, RoundedCornerShape(4.dp))
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(dev.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(
                                        text = if (dev.isBluetooth) "Bluetooth LE MIDI" else "Interface USB MIDI · ${dev.inputPortCount} In / ${dev.outputPortCount} Out",
                                        fontSize = 8.sp,
                                        color = TextMuted
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(NeonGreen.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("CONECTADO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Nenhum teclado USB MIDI detectado. Conecte via cabo OTG para detecção automática.",
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // 2. Bluetooth LE MIDI Keyboards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
                    .background(StudioSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bluetooth, contentDescription = "BLE MIDI", tint = NeonPurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TECLADOS BLUETOOTH LE MIDI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                    }

                    Button(
                        onClick = {
                            if (isScanning) {
                                midiHelper.stopBleMidiScan()
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH_SCAN,
                                            Manifest.permission.BLUETOOTH_CONNECT
                                        )
                                    )
                                } else {
                                    midiHelper.startBleMidiScan()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) NeonRed else NeonPurple
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            text = if (isScanning) "PARAR BUSCA" else "BUSCAR DISPOSITIVOS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (discoveredBle.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(discoveredBle) { bleDev ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StudioCard, RoundedCornerShape(4.dp))
                                    .border(1.dp, StudioBorder, RoundedCornerShape(4.dp))
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(bleDev.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("${bleDev.address} · RSSI: ${bleDev.rssi} dBm", fontSize = 8.sp, color = TextMuted)
                                }

                                Button(
                                    onClick = { midiHelper.connectBleDevice(bleDev.device) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                    shape = RoundedCornerShape(3.dp),
                                    modifier = Modifier.height(22.dp)
                                ) {
                                    Text("CONECTAR", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isScanning) "Procurando teclados e controladores BLE MIDI..." else "Toque em Buscar Dispositivos para emparelhar teclado Bluetooth externo.",
                            fontSize = 9.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Right Column: Remote Control Mappings & MIDI Export
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Live MIDI Event Monitor
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "MONITOR MIDI EM TEMPO REAL",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonAmber
                )
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioCard, RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = lastEvent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                }
            }

            // Remote Control Mapping Specs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(StudioSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "CONTROLE REMOTO MIDI (CC)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(
                        "CC 7" to "Volume do Canal vinculado",
                        "CC 10" to "Panorâmica (Pan) do Canal",
                        "CC 74" to "Filtro de Corte (Cutoff Hz)",
                        "CC 71" to "Ressonância do Filtro (Q)",
                        "CC 114" to "Play / Pause Remoto",
                        "CC 115" to "Tap Tempo Remoto"
                    ).forEach { (cc, desc) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cc, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                            Text(desc, fontSize = 8.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // MIDI File Exporter Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Exportar", tint = NeonGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EXPORTAÇÃO PADRÃO MIDI (.MID)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Exporta o padrão de 16 passos e as 16 faixas em formato Standard MIDI File (SMF Tipo 1) para sua DAW favorita.",
                    fontSize = 8.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(6.dp))

                exportMessage?.let { msg ->
                    Text(text = msg, fontSize = 8.sp, color = NeonGreen, modifier = Modifier.padding(bottom = 4.dp))
                }

                Button(
                    onClick = {
                        val grid = sequencer.getAllGrid()
                        val tracks = audioEngine.tracksFlow.value
                        val result = MidiExporter.exportPatternToMidiFile(
                            context = context,
                            grid = grid,
                            tracks = tracks,
                            bpm = bpm
                        )
                        result.fold(
                            onSuccess = { file ->
                                exportMessage = "Arquivo gerado: ${file.name}"
                                MidiExporter.shareMidiFile(context, file)
                            },
                            onFailure = { err ->
                                exportMessage = "Falha ao exportar: ${err.localizedMessage}"
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Exportar", tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "EXPORTAR / COMPARTILHAR .MID",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
