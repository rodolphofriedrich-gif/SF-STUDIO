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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngine
import com.example.sequencer.StepSequencer
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
import kotlin.random.Random

@Composable
fun SequencerScreen(
    sequencer: StepSequencer,
    audioEngine: AudioEngine,
    selectedTrackId: Int,
    onSelectTrack: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStep by sequencer.currentStep.collectAsState()
    val isPlaying by sequencer.isPlaying.collectAsState()
    val gridVersion by sequencer.gridVersion.collectAsState()
    val tracks by audioEngine.tracksFlow.collectAsState()

    var selectedStepIndex by remember { mutableIntStateOf(0) }
    var presetMenuOpen by remember { mutableStateOf(false) }

    val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun midiToNoteString(midi: Int): String {
        val octave = (midi / 12) - 1
        val noteIdx = midi % 12
        return "${noteNames[noteIdx]}$octave"
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left Column: 16 Tracks Selector List
        Column(
            modifier = Modifier
                .width(160.dp)
                .fillMaxHeight()
                .background(StudioSurface, RoundedCornerShape(8.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FAIXAS (16)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )

                // Pattern Presets Dropdown button
                Box {
                    Text(
                        text = "PADRÕES ▾",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber,
                        modifier = Modifier
                            .background(StudioCard, RoundedCornerShape(4.dp))
                            .clickable { presetMenuOpen = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    DropdownMenu(
                        expanded = presetMenuOpen,
                        onDismissRequest = { presetMenuOpen = false },
                        modifier = Modifier.background(StudioCard)
                    ) {
                        listOf("Synthwave", "Trap 808", "House", "Lo-Fi").forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset, color = TextPrimary, fontSize = 11.sp) },
                                onClick = {
                                    sequencer.loadPatternPreset(preset)
                                    presetMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                itemsIndexed(tracks) { idx, track ->
                    val isSelected = idx == selectedTrackId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) StudioHighlight else StudioCard)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) NeonCyan else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onSelectTrack(idx) }
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${idx + 1}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeonCyan else TextMuted,
                                modifier = Modifier.width(16.dp)
                            )
                            Text(
                                text = track.name,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                maxLines = 1
                            )
                        }

                        // Mute & Solo mini pills
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(
                                        if (track.isMuted) NeonRed else StudioSurface,
                                        RoundedCornerShape(2.dp)
                                    )
                                    .clickable {
                                        audioEngine.updateTrack(idx) { it.copy(isMuted = !it.isMuted) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("M", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(
                                        if (track.isSolo) NeonAmber else StudioSurface,
                                        RoundedCornerShape(2.dp)
                                    )
                                    .clickable {
                                        audioEngine.updateTrack(idx) { it.copy(isSolo = !it.isSolo) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("S", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }

        // Center Column: 16-Step Grid for Current Track + Step Actions
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val track = tracks[selectedTrackId]

            // Track Header & Action Bar
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
                        text = "FAIXA ${selectedTrackId + 1}: ${track.name.uppercase()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Canal MIDI: ${if (track.midiChannel == 0) "OMNI" else track.midiChannel}",
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Randomize Steps
                    Row(
                        modifier = Modifier
                            .background(StudioCard, RoundedCornerShape(4.dp))
                            .clickable {
                                for (s in 0..15) {
                                    val shouldActive = Random.nextFloat() > 0.65f
                                    sequencer.updateStep(selectedTrackId, s) {
                                        it.copy(active = shouldActive, velocity = Random.nextInt(70, 127))
                                    }
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Randomizar", tint = NeonAmber, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("RANDOM", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                    }

                    // Clear Track Steps
                    Row(
                        modifier = Modifier
                            .background(StudioCard, RoundedCornerShape(4.dp))
                            .clickable { sequencer.clearTrack(selectedTrackId) }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = NeonRed, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("LIMPAR", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonRed)
                    }
                }
            }

            // 16-Step Interactive Pad Buttons Matrix
            // 2 rows of 8 steps for optimal landscape touch ergonomics
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val stepRows = listOf(
                    (0..7).toList(),
                    (8..15).toList()
                )

                for (row in stepRows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (step in row) {
                            val stepData = sequencer.getStep(selectedTrackId, step)
                            val isCurrentRunning = isPlaying && currentStep == step
                            val isSelectedInspector = selectedStepIndex == step
                            val isDownbeat = step % 4 == 0

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            stepData.active && isCurrentRunning -> Color.White
                                            stepData.active -> NeonAmber
                                            isCurrentRunning -> NeonCyan.copy(alpha = 0.35f)
                                            isDownbeat -> StudioHighlight
                                            else -> StudioCard
                                        }
                                    )
                                    .border(
                                        width = if (isSelectedInspector) 2.dp else if (isCurrentRunning) 1.5.dp else 1.dp,
                                        color = if (isSelectedInspector) NeonCyan else if (isCurrentRunning) Color.White else StudioBorder,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .pointerInput(selectedTrackId, step, gridVersion) {
                                        detectTapGestures(
                                            onTap = {
                                                selectedStepIndex = step
                                                sequencer.toggleStep(selectedTrackId, step)
                                            },
                                            onLongPress = {
                                                selectedStepIndex = step
                                            }
                                        )
                                    }
                                    .padding(3.dp)
                            ) {
                                // Step number (top)
                                Text(
                                    text = "${step + 1}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (stepData.active) Color.Black else TextMuted,
                                    modifier = Modifier.align(Alignment.TopStart)
                                )

                                // Note Name (center)
                                if (stepData.active) {
                                    Text(
                                        text = midiToNoteString(stepData.note),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }

                                // Velocity bar at bottom
                                if (stepData.active) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(1.dp))
                                            .align(Alignment.BottomCenter)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(stepData.velocity / 127f)
                                                .fillMaxHeight()
                                                .background(Color.White, RoundedCornerShape(1.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Step Pitch & Velocity Inspector Toolbar
            val inspectedStep = sequencer.getStep(selectedTrackId, selectedStepIndex)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PASSO ${selectedStepIndex + 1} INSPECTOR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Text(
                        text = "Nota: ${midiToNoteString(inspectedStep.note)} (${inspectedStep.note})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Quick Semitone Note Transpose
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "-12",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier
                            .background(StudioCard, RoundedCornerShape(4.dp))
                            .clickable {
                                sequencer.updateStep(selectedTrackId, selectedStepIndex) {
                                    it.copy(note = (it.note - 12).coerceIn(0, 127))
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "-1",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier
                            .background(StudioCard, RoundedCornerShape(4.dp))
                            .clickable {
                                sequencer.updateStep(selectedTrackId, selectedStepIndex) {
                                    it.copy(note = (it.note - 1).coerceIn(0, 127))
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "+1",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier
                            .background(StudioCard, RoundedCornerShape(4.dp))
                            .clickable {
                                sequencer.updateStep(selectedTrackId, selectedStepIndex) {
                                    it.copy(note = (it.note + 1).coerceIn(0, 127))
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "+12",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier
                            .background(StudioCard, RoundedCornerShape(4.dp))
                            .clickable {
                                sequencer.updateStep(selectedTrackId, selectedStepIndex) {
                                    it.copy(note = (it.note + 12).coerceIn(0, 127))
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                // Step Velocity Stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "VEL: ${inspectedStep.velocity}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                        Text(text = "GATE: ${(inspectedStep.gate * 100).toInt()}%", fontSize = 8.sp, color = TextMuted)
                    }

                    RotaryKnob(
                        value = inspectedStep.velocity / 127f,
                        onValueChange = { norm ->
                            sequencer.updateStep(selectedTrackId, selectedStepIndex) {
                                it.copy(velocity = (norm * 127).toInt().coerceIn(1, 127))
                            }
                        },
                        label = "VEL",
                        valueDisplay = "${inspectedStep.velocity}",
                        size = 32.dp,
                        tintColor = NeonGreen
                    )
                }
            }
        }
    }
}
