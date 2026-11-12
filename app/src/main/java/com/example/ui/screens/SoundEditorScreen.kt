package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngine
import com.example.audio.FilterType
import com.example.soundfont.SoundBankManager
import com.example.ui.components.ADSRVisualizer
import com.example.ui.components.FilterVisualizer
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
fun SoundEditorScreen(
    audioEngine: AudioEngine,
    soundBankManager: SoundBankManager,
    selectedTrackId: Int,
    onSelectTrack: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tracks by audioEngine.tracksFlow.collectAsState()
    val track = tracks[selectedTrackId]
    val allSamples = remember { soundBankManager.getAllSamples() }

    var sampleDropdownOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Track & Instrument Selector Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioSurface, RoundedCornerShape(8.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "FAIXA ${selectedTrackId + 1}: ${track.name}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Track Selector Tabs
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (0..15).forEach { tId ->
                        val isCurr = tId == selectedTrackId
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isCurr) NeonCyan else StudioCard)
                                .clickable { onSelectTrack(tId) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${tId + 1}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurr) Color.Black else TextSecondary
                            )
                        }
                    }
                }
            }

            // Sample / SoundFont Instrument Switcher
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Row(
                        modifier = Modifier
                            .background(StudioCard, RoundedCornerShape(4.dp))
                            .border(1.dp, StudioBorder, RoundedCornerShape(4.dp))
                            .clickable { sampleDropdownOpen = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = "Amostra", tint = NeonAmber, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = track.soundName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = sampleDropdownOpen,
                        onDismissRequest = { sampleDropdownOpen = false },
                        modifier = Modifier.background(StudioCard)
                    ) {
                        allSamples.forEach { sample ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(sample.name, fontSize = 11.sp, color = TextPrimary)
                                        Text("${sample.sourceFormat} · Nota ${sample.rootKey}", fontSize = 8.sp, color = TextMuted)
                                    }
                                },
                                onClick = {
                                    audioEngine.updateTrack(selectedTrackId) {
                                        it.copy(soundId = sample.id, soundName = sample.name)
                                    }
                                    sampleDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Test Note Play Button
                Box(
                    modifier = Modifier
                        .background(NeonGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .border(1.dp, NeonGreen, RoundedCornerShape(4.dp))
                        .pointerInput(selectedTrackId) {
                            detectTapGestures(
                                onPress = {
                                    audioEngine.noteOn(selectedTrackId, 60, 1.0f)
                                    tryAwaitRelease()
                                    audioEngine.noteOff(selectedTrackId, 60)
                                }
                            )
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("TESTAR NOTA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                }
            }
        }

        // Main Editor Panes: ADSR on Left, Filter & Pitch on Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. ADSR Envelope Editor
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(StudioSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ENVELOPE ADSR (AMPLITUDE)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "A:${(track.adsr.attack * 1000).toInt()}ms · D:${(track.adsr.decay * 1000).toInt()}ms · S:${(track.adsr.sustain * 100).toInt()}% · R:${(track.adsr.release * 1000).toInt()}ms",
                        fontSize = 8.sp,
                        color = TextMuted
                    )
                }

                // Interactive Canvas Visualizer
                ADSRVisualizer(
                    adsr = track.adsr,
                    height = 95.dp,
                    tintColor = NeonCyan
                )

                // 4 Knobs for Attack, Decay, Sustain, Release
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Attack (1ms to 3000ms)
                    RotaryKnob(
                        value = (track.adsr.attack / 3.0f).coerceIn(0f, 1f),
                        onValueChange = { norm ->
                            val sec = 0.001f + norm * norm * 3.0f
                            audioEngine.updateTrack(selectedTrackId) {
                                it.copy(adsr = it.adsr.copy(attack = sec))
                            }
                        },
                        label = "ATTACK",
                        valueDisplay = "${(track.adsr.attack * 1000).toInt()}ms",
                        tintColor = NeonCyan
                    )

                    // Decay (1ms to 3000ms)
                    RotaryKnob(
                        value = (track.adsr.decay / 3.0f).coerceIn(0f, 1f),
                        onValueChange = { norm ->
                            val sec = 0.001f + norm * norm * 3.0f
                            audioEngine.updateTrack(selectedTrackId) {
                                it.copy(adsr = it.adsr.copy(decay = sec))
                            }
                        },
                        label = "DECAY",
                        valueDisplay = "${(track.adsr.decay * 1000).toInt()}ms",
                        tintColor = NeonCyan
                    )

                    // Sustain (0% to 100%)
                    RotaryKnob(
                        value = track.adsr.sustain.coerceIn(0f, 1f),
                        onValueChange = { norm ->
                            audioEngine.updateTrack(selectedTrackId) {
                                it.copy(adsr = it.adsr.copy(sustain = norm))
                            }
                        },
                        label = "SUSTAIN",
                        valueDisplay = "${(track.adsr.sustain * 100).toInt()}%",
                        tintColor = NeonCyan
                    )

                    // Release (10ms to 5000ms)
                    RotaryKnob(
                        value = (track.adsr.release / 5.0f).coerceIn(0f, 1f),
                        onValueChange = { norm ->
                            val sec = 0.01f + norm * norm * 5.0f
                            audioEngine.updateTrack(selectedTrackId) {
                                it.copy(adsr = it.adsr.copy(release = sec))
                            }
                        },
                        label = "RELEASE",
                        valueDisplay = "${(track.adsr.release * 1000).toInt()}ms",
                        tintColor = NeonCyan
                    )
                }
            }

            // 2. Resonant Filter & Pitch Shaping
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(StudioSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FILTRO DE CORTE & RESSONÂNCIA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber,
                        letterSpacing = 1.sp
                    )

                    // Filter Type Buttons (LP, HP, BP)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf(
                            FilterType.LOW_PASS to "LP",
                            FilterType.HIGH_PASS to "HP",
                            FilterType.BAND_PASS to "BP"
                        ).forEach { (type, label) ->
                            val isSel = track.filter.type == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSel) NeonAmber else StudioCard)
                                    .clickable {
                                        audioEngine.updateTrack(selectedTrackId) {
                                            it.copy(filter = it.filter.copy(type = type))
                                        }
                                    }
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.Black else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Interactive Frequency Response Canvas
                FilterVisualizer(
                    filter = track.filter,
                    height = 95.dp,
                    tintColor = NeonAmber
                )

                // Filter & Pitch Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Cutoff Hz (20 to 20000 Hz)
                    RotaryKnob(
                        value = (track.filter.cutoffHz / 20000f).coerceIn(0f, 1f),
                        onValueChange = { norm ->
                            val hz = 20f + norm * norm * 19980f
                            audioEngine.updateTrack(selectedTrackId) {
                                it.copy(filter = it.filter.copy(cutoffHz = hz))
                            }
                        },
                        label = "CUTOFF",
                        valueDisplay = "${track.filter.cutoffHz.toInt()}Hz",
                        tintColor = NeonAmber
                    )

                    // Resonance Q (0.1 to 8.0)
                    RotaryKnob(
                        value = ((track.filter.resonance - 0.1f) / 7.9f).coerceIn(0f, 1f),
                        onValueChange = { norm ->
                            val q = 0.1f + norm * 7.9f
                            audioEngine.updateTrack(selectedTrackId) {
                                it.copy(filter = it.filter.copy(resonance = q))
                            }
                        },
                        label = "RESONANCE",
                        valueDisplay = "Q ${String.format("%.1f", track.filter.resonance)}",
                        tintColor = NeonAmber
                    )

                    // Transpose (-24 to +24 semitones)
                    RotaryKnob(
                        value = (track.transpose + 24) / 48f,
                        onValueChange = { norm ->
                            val st = (norm * 48 - 24).toInt()
                            audioEngine.updateTrack(selectedTrackId) {
                                it.copy(transpose = st)
                            }
                        },
                        label = "TRANSPOSE",
                        valueDisplay = "${if (track.transpose > 0) "+${track.transpose}" else track.transpose}st",
                        tintColor = NeonGreen
                    )

                    // Choke Group
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            val nextGroup = (track.chokeGroup + 1) % 5
                            audioEngine.updateTrack(selectedTrackId) { it.copy(chokeGroup = nextGroup) }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(StudioCard, RoundedCornerShape(6.dp))
                                .border(1.dp, if (track.chokeGroup > 0) NeonPurple else StudioBorder, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (track.chokeGroup == 0) "OFF" else "G${track.chokeGroup}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (track.chokeGroup > 0) NeonPurple else TextMuted
                            )
                        }
                        Text("CHOKE", fontSize = 8.sp, color = TextSecondary)
                        Text(if (track.chokeGroup == 0) "None" else "Grupo ${track.chokeGroup}", fontSize = 7.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}
