package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEngine
import com.example.pads.DefaultPads
import com.example.pads.PerformancePad
import com.example.ui.components.RotaryKnob
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioHighlight
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PadsScreen(
    audioEngine: AudioEngine,
    selectedTrackId: Int,
    onSelectTrack: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    var fullLevelMode by remember { mutableStateOf(false) }
    var octaveShift by remember { mutableIntStateOf(0) }
    var pressedPadIndex by remember { mutableIntStateOf(-1) }
    var lastTriggeredVelocity by remember { mutableFloatStateOf(1.0f) }

    val pads = remember { DefaultPads.defaultList.toMutableList() }

    fun triggerPad(padIndex: Int, normalizedY: Float) {
        val pad = pads[padIndex]
        val velocity = if (fullLevelMode) 1.0f else (1.0f - normalizedY * 0.7f).coerceIn(0.2f, 1.0f)
        lastTriggeredVelocity = velocity
        pressedPadIndex = padIndex
        onSelectTrack(pad.trackId)

        // Haptic feedback
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(15, (velocity * 255).toInt().coerceIn(40, 255)))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(15)
            }
        } catch (_: Exception) {}

        val note = (pad.midiNote + octaveShift * 12).coerceIn(0, 127)
        audioEngine.noteOn(pad.trackId, note, velocity)
    }

    fun releasePad(padIndex: Int) {
        if (pressedPadIndex == padIndex) {
            pressedPadIndex = -1
        }
        val pad = pads[padIndex]
        val note = (pad.midiNote + octaveShift * 12).coerceIn(0, 127)
        audioEngine.noteOff(pad.trackId, note)
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left: 4x4 Dynamic Performance Pads Grid
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 4 rows of 4 pads (MPC style: Row 0 is pads 12-15, Row 3 is pads 0-3)
            val rowIndices = listOf(
                listOf(12, 13, 14, 15),
                listOf(8, 9, 10, 11),
                listOf(4, 5, 6, 7),
                listOf(0, 1, 2, 3)
            )

            for (row in rowIndices) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (padIdx in row) {
                        val pad = pads[padIdx]
                        val isPressed = pressedPadIndex == padIdx
                        val isCurrentTrack = selectedTrackId == pad.trackId

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isPressed) pad.color.copy(alpha = 0.5f)
                                    else if (isCurrentTrack) StudioCard
                                    else StudioSurface
                                )
                                .border(
                                    width = if (isPressed) 2.5.dp else if (isCurrentTrack) 1.5.dp else 1.dp,
                                    color = if (isPressed) Color.White else if (isCurrentTrack) pad.color else StudioBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .pointerInput(padIdx, fullLevelMode, octaveShift) {
                                    detectTapGestures(
                                        onPress = { offset ->
                                            val normY = (offset.y / size.height).coerceIn(0f, 1f)
                                            triggerPad(padIdx, normY)
                                            tryAwaitRelease()
                                            releasePad(padIdx)
                                        }
                                    )
                                }
                                .padding(6.dp)
                        ) {
                            // Pad number indicator (top-left)
                            Text(
                                text = "P${padIdx + 1}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = pad.color,
                                modifier = Modifier.align(Alignment.TopStart)
                            )

                            // Track Number (top-right)
                            Text(
                                text = "TRK ${pad.trackId + 1}",
                                fontSize = 8.sp,
                                color = TextMuted,
                                modifier = Modifier.align(Alignment.TopEnd)
                            )

                            // Pad Name (center)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Text(
                                    text = pad.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPressed) Color.White else TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Nota ${(pad.midiNote + octaveShift * 12)}",
                                    fontSize = 8.sp,
                                    color = TextMuted
                                )
                            }

                            // Dynamic touch velocity indicator bar (bottom)
                            if (isPressed) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(lastTriggeredVelocity)
                                        .height(3.dp)
                                        .background(pad.color, RoundedCornerShape(2.dp))
                                        .align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right: Performance & Quick Tweak Panel
        Column(
            modifier = Modifier
                .width(170.dp)
                .fillMaxHeight()
                .background(StudioSurface, RoundedCornerShape(8.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "PAD CONTROLS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 1.sp
            )

            // Mode Toggles: Full Level 127
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (fullLevelMode) NeonAmber.copy(alpha = 0.2f) else StudioCard)
                    .border(1.dp, if (fullLevelMode) NeonAmber else StudioBorder, RoundedCornerShape(6.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { fullLevelMode = !fullLevelMode }
                    }
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "FULL LEVEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    text = if (fullLevelMode) "127 FIXO" else "DINÂMICO",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (fullLevelMode) NeonAmber else TextSecondary
                )
            }

            // Octave Transpose
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioCard, RoundedCornerShape(6.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(StudioHighlight, RoundedCornerShape(4.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { octaveShift = (octaveShift - 1).coerceAtLeast(-2) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "-", fontWeight = FontWeight.Bold, color = NeonCyan)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${if (octaveShift >= 0) "+$octaveShift" else "$octaveShift"} OCT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "OITAVA", fontSize = 7.sp, color = TextMuted)
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(StudioHighlight, RoundedCornerShape(4.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { octaveShift = (octaveShift + 1).coerceAtMost(2) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+", fontWeight = FontWeight.Bold, color = NeonCyan)
                }
            }

            // Selected Track Quick Controls (Cutoff and Volume)
            val currentTrack = audioEngine.getTrack(selectedTrackId)
            Text(
                text = "TRK ${selectedTrackId + 1}: ${currentTrack.name}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                maxLines = 1
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RotaryKnob(
                    value = (currentTrack.filter.cutoffHz / 20000f).coerceIn(0f, 1f),
                    onValueChange = { norm ->
                        val hz = 40f + norm * norm * 19960f
                        audioEngine.updateTrack(selectedTrackId) {
                            it.copy(filter = it.filter.copy(cutoffHz = hz))
                        }
                    },
                    label = "CUTOFF",
                    valueDisplay = "${currentTrack.filter.cutoffHz.toInt()}Hz",
                    size = 38.dp,
                    tintColor = NeonAmber
                )

                RotaryKnob(
                    value = (currentTrack.volume / 1.5f).coerceIn(0f, 1f),
                    onValueChange = { norm ->
                        audioEngine.updateTrack(selectedTrackId) {
                            it.copy(volume = norm * 1.5f)
                        }
                    },
                    label = "VOLUME",
                    valueDisplay = "${(currentTrack.volume * 100).toInt()}%",
                    size = 38.dp,
                    tintColor = NeonCyan
                )
            }

            // Real-time Velocity Monitor
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioCard, RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "TOUCH VELOCITY", fontSize = 8.sp, color = TextMuted)
                    Text(text = "${(lastTriggeredVelocity * 127).toInt()}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(StudioBorder, RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(lastTriggeredVelocity)
                            .fillMaxHeight()
                            .background(NeonGreen, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}
