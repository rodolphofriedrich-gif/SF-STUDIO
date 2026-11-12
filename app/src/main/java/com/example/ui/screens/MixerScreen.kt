package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngine
import com.example.audio.TrackChannel
import com.example.ui.components.MixerFader
import com.example.ui.components.RotaryKnob
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
fun MixerScreen(
    audioEngine: AudioEngine,
    selectedTrackId: Int,
    onSelectTrack: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tracks by audioEngine.tracksFlow.collectAsState()
    val trackPeaks by audioEngine.trackVUMeters.collectAsState()
    val masterPeak by audioEngine.masterVUMeter.collectAsState()

    var showEqFxDialogForTrackId by remember { mutableStateOf<Int?>(null) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 16 Channels Horizontal Scroll Console
        val scrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tracks.forEachIndexed { idx, track ->
                val peak = trackPeaks.getOrElse(idx) { 0f }
                val isSelected = idx == selectedTrackId

                ChannelStrip(
                    track = track,
                    peak = peak,
                    isSelected = isSelected,
                    onSelect = { onSelectTrack(idx) },
                    onVolumeChange = { vol ->
                        audioEngine.updateTrack(idx) { it.copy(volume = vol) }
                    },
                    onPanChange = { pan ->
                        audioEngine.updateTrack(idx) { it.copy(pan = pan) }
                    },
                    onMuteToggle = {
                        audioEngine.updateTrack(idx) { it.copy(isMuted = !it.isMuted) }
                    },
                    onSoloToggle = {
                        audioEngine.updateTrack(idx) { it.copy(isSolo = !it.isSolo) }
                    },
                    onMidiChannelChange = { midiCh ->
                        audioEngine.updateTrack(idx) { it.copy(midiChannel = midiCh) }
                    },
                    onOpenFx = { showEqFxDialogForTrackId = idx }
                )
            }
        }

        // Master Output Channel Strip
        MasterChannelStrip(
            masterVolume = audioEngine.masterVolume,
            onMasterVolumeChange = { audioEngine.masterVolume = it },
            peakL = masterPeak.first,
            peakR = masterPeak.second
        )
    }

    // Parametric EQ & Channel FX Dialog
    showEqFxDialogForTrackId?.let { trackId ->
        val track = tracks[trackId]
        ChannelEqFxDialog(
            track = track,
            onDismiss = { showEqFxDialogForTrackId = null },
            onUpdateTrack = { updated ->
                audioEngine.updateTrack(trackId) { updated }
            }
        )
    }
}

@Composable
fun ChannelStrip(
    track: TrackChannel,
    peak: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onPanChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onSoloToggle: () -> Unit,
    onMidiChannelChange: (Int) -> Unit,
    onOpenFx: () -> Unit,
    modifier: Modifier = Modifier
) {
    var midiDropdownOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(66.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) StudioHighlight else StudioSurface)
            .border(
                width = 1.dp,
                color = if (isSelected) NeonCyan else StudioBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onSelect() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Track Number & Name
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TRK ${track.id + 1}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) NeonCyan else TextPrimary
            )
            Text(
                text = track.name,
                fontSize = 8.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }

        // MIDI Channel Selector Pill
        Box {
            Text(
                text = if (track.midiChannel == 0) "MIDI: ALL" else "MIDI: ${track.midiChannel}",
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = NeonAmber,
                modifier = Modifier
                    .background(StudioCard, RoundedCornerShape(3.dp))
                    .clickable { midiDropdownOpen = true }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            DropdownMenu(
                expanded = midiDropdownOpen,
                onDismissRequest = { midiDropdownOpen = false },
                modifier = Modifier.background(StudioCard)
            ) {
                DropdownMenuItem(
                    text = { Text("OMNI (Todos Canais)", fontSize = 10.sp, color = TextPrimary) },
                    onClick = {
                        onMidiChannelChange(0)
                        midiDropdownOpen = false
                    }
                )
                (1..16).forEach { ch ->
                    DropdownMenuItem(
                        text = { Text("Canal $ch", fontSize = 10.sp, color = TextPrimary) },
                        onClick = {
                            onMidiChannelChange(ch)
                            midiDropdownOpen = false
                        }
                    )
                }
            }
        }

        // FX & EQ Dialog Launcher Button
        Row(
            modifier = Modifier
                .background(StudioCard, RoundedCornerShape(3.dp))
                .border(0.5.dp, StudioBorder, RoundedCornerShape(3.dp))
                .clickable { onOpenFx() }
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Tune, contentDescription = "FX", tint = NeonCyan, modifier = Modifier.size(10.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text("EQ/FX", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
        }

        // Pan Knob
        RotaryKnob(
            value = (track.pan + 1.0f) * 0.5f,
            onValueChange = { norm -> onPanChange((norm * 2.0f) - 1.0f) },
            label = "PAN",
            valueDisplay = when {
                track.pan < -0.05f -> "L${(-track.pan * 100).toInt()}"
                track.pan > 0.05f -> "R${(track.pan * 100).toInt()}"
                else -> "C"
            },
            size = 28.dp,
            tintColor = NeonAmber
        )

        // Mute & Solo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (track.isMuted) NeonRed else StudioCard)
                    .clickable { onMuteToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text("M", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (track.isMuted) Color.White else TextMuted)
            }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (track.isSolo) NeonAmber else StudioCard)
                    .clickable { onSoloToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text("S", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (track.isSolo) Color.Black else TextMuted)
            }
        }

        // Volume Fader & Peak VU Meter
        MixerFader(
            volume = track.volume,
            peakLevel = peak,
            onVolumeChange = onVolumeChange,
            height = 110.dp
        )

        // Volume readout
        Text(
            text = "${(track.volume * 100).toInt()}%",
            fontSize = 8.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun MasterChannelStrip(
    masterVolume: Float,
    onMasterVolumeChange: (Float) -> Unit,
    peakL: Float,
    peakR: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(72.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(StudioCard)
            .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "MASTER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonCyan)
            Text(text = "STEREO OUT", fontSize = 7.sp, color = TextMuted)
        }

        // Limiter Active Indicator
        Box(
            modifier = Modifier
                .background(StudioSurface, RoundedCornerShape(4.dp))
                .border(0.5.dp, StudioBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(text = "LIMITER ON", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
        }

        // Master Fader & Stereo Meters
        Row(
            modifier = Modifier.height(130.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MixerFader(
                volume = masterVolume,
                peakLevel = (peakL + peakR) * 0.5f,
                onVolumeChange = onMasterVolumeChange,
                height = 130.dp
            )
        }

        Text(text = "${(masterVolume * 100).toInt()}%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
    }
}

/**
 * 3-Band Parametric EQ and Real-Time Channel Processing FX Dialog
 */
@Composable
fun ChannelEqFxDialog(
    track: TrackChannel,
    onDismiss: () -> Unit,
    onUpdateTrack: (TrackChannel) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROCESSAMENTO E EQ · FAIXA ${track.id + 1}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Equalização Paramétrica de 3 Bandas
                Text(
                    text = "EQUALIZADOR PARAMÉTRICO DE 3 BANDAS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonAmber
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioSurface, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Low Shelf
                    RotaryKnob(
                        value = (track.fx.eq.lowShelfGainDb + 12f) / 24f,
                        onValueChange = { norm ->
                            val gain = (norm * 24f) - 12f
                            onUpdateTrack(track.copy(fx = track.fx.copy(eq = track.fx.eq.copy(lowShelfGainDb = gain))))
                        },
                        label = "LOW 100Hz",
                        valueDisplay = "${track.fx.eq.lowShelfGainDb.toInt()}dB",
                        tintColor = NeonCyan
                    )

                    // Mid Frequency & Gain
                    RotaryKnob(
                        value = (track.fx.eq.midFreqHz - 200f) / 4800f,
                        onValueChange = { norm ->
                            val freq = 200f + norm * 4800f
                            onUpdateTrack(track.copy(fx = track.fx.copy(eq = track.fx.eq.copy(midFreqHz = freq))))
                        },
                        label = "MID FREQ",
                        valueDisplay = "${track.fx.eq.midFreqHz.toInt()}Hz",
                        tintColor = NeonAmber
                    )

                    RotaryKnob(
                        value = (track.fx.eq.midGainDb + 12f) / 24f,
                        onValueChange = { norm ->
                            val gain = (norm * 24f) - 12f
                            onUpdateTrack(track.copy(fx = track.fx.copy(eq = track.fx.eq.copy(midGainDb = gain))))
                        },
                        label = "MID GAIN",
                        valueDisplay = "${track.fx.eq.midGainDb.toInt()}dB",
                        tintColor = NeonAmber
                    )

                    // High Shelf
                    RotaryKnob(
                        value = (track.fx.eq.highShelfGainDb + 12f) / 24f,
                        onValueChange = { norm ->
                            val gain = (norm * 24f) - 12f
                            onUpdateTrack(track.copy(fx = track.fx.copy(eq = track.fx.eq.copy(highShelfGainDb = gain))))
                        },
                        label = "HIGH 8kHz",
                        valueDisplay = "${track.fx.eq.highShelfGainDb.toInt()}dB",
                        tintColor = NeonGreen
                    )
                }

                // 2. Efeitos Integrados (Drive, Delay, Reverb)
                Text(
                    text = "EFEITOS DE PROCESSAMENTO EM TEMPO REAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPurple
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioSurface, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RotaryKnob(
                        value = track.fx.drive,
                        onValueChange = { onUpdateTrack(track.copy(fx = track.fx.copy(drive = it))) },
                        label = "SATURAÇÃO",
                        valueDisplay = "${(track.fx.drive * 100).toInt()}%",
                        tintColor = NeonRed
                    )

                    RotaryKnob(
                        value = track.fx.delayWet,
                        onValueChange = { onUpdateTrack(track.copy(fx = track.fx.copy(delayWet = it))) },
                        label = "DELAY WET",
                        valueDisplay = "${(track.fx.delayWet * 100).toInt()}%",
                        tintColor = NeonCyan
                    )

                    RotaryKnob(
                        value = (track.fx.delayTimeMs - 20f) / 980f,
                        onValueChange = { norm ->
                            val ms = 20f + norm * 980f
                            onUpdateTrack(track.copy(fx = track.fx.copy(delayTimeMs = ms)))
                        },
                        label = "DELAY TEMPO",
                        valueDisplay = "${track.fx.delayTimeMs.toInt()}ms",
                        tintColor = NeonCyan
                    )

                    RotaryKnob(
                        value = track.fx.reverbWet,
                        onValueChange = { onUpdateTrack(track.copy(fx = track.fx.copy(reverbWet = it))) },
                        label = "REVERB WET",
                        valueDisplay = "${(track.fx.reverbWet * 100).toInt()}%",
                        tintColor = NeonPurple
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("PRONTO", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = StudioCard
    )
}
