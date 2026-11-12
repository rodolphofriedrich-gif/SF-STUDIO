package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioHighlight
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TransportBar(
    bpm: Int,
    onBpmChange: (Int) -> Unit,
    onTapTempo: () -> Unit,
    swing: Float,
    onSwingChange: (Float) -> Unit,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    currentStep: Int,
    masterVolume: Float,
    onMasterVolumeChange: (Float) -> Unit,
    masterPeakL: Float,
    masterPeakR: Float,
    isMidiConnected: Boolean,
    onMidiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioSurface,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .border(width = 1.dp, color = StudioBorder)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            // App Branding & Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .background(StudioCard, RoundedCornerShape(6.dp))
                        .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = "SF",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "SF STUDIO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "SF2 · SF3 · SFZ",
                        fontSize = 8.sp,
                        color = TextMuted
                    )
                }
            }

            // BPM & Tap Tempo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(StudioCard, RoundedCornerShape(6.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "-",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier
                        .clickable { onBpmChange(bpm - 1) }
                        .padding(horizontal = 6.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$bpm",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "BPM",
                        fontSize = 7.sp,
                        color = TextMuted
                    )
                }
                Text(
                    text = "+",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    modifier = Modifier
                        .clickable { onBpmChange(bpm + 1) }
                        .padding(horizontal = 6.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .background(StudioHighlight, RoundedCornerShape(4.dp))
                        .clickable { onTapTempo() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "TAP",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))
                // Swing
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        val nextSwing = when {
                            swing < 0.1f -> 0.25f
                            swing < 0.35f -> 0.50f
                            swing < 0.60f -> 0.65f
                            else -> 0.0f
                        }
                        onSwingChange(nextSwing)
                    }
                ) {
                    Text(
                        text = "${(swing * 100).toInt()}%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (swing > 0f) NeonCyan else TextMuted
                    )
                    Text(
                        text = "SWING",
                        fontSize = 7.sp,
                        color = TextMuted
                    )
                }
            }

            // Transport Buttons (Play, Pause, Stop)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Play / Pause
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isPlaying) NeonGreen.copy(alpha = 0.2f) else StudioCard,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (isPlaying) NeonGreen else StudioBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { if (isPlaying) onPause() else onPlay() }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = if (isPlaying) NeonGreen else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Stop
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(StudioCard, RoundedCornerShape(6.dp))
                        .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                        .clickable { onStop() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 16-Step Glowing LED Ticker
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .background(StudioCard, RoundedCornerShape(6.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                for (s in 0..15) {
                    val isCurrent = s == currentStep && isPlaying
                    val isDownbeat = s % 4 == 0
                    Box(
                        modifier = Modifier
                            .size(width = 8.dp, height = 14.dp)
                            .background(
                                color = when {
                                    isCurrent -> NeonAmber
                                    isDownbeat -> StudioHighlight
                                    else -> StudioSurface
                                },
                                shape = RoundedCornerShape(2.dp)
                            )
                            .border(
                                width = 0.5.dp,
                                color = if (isCurrent) Color.White else StudioBorder,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            // Master Volume & Stereo VU Meter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RotaryKnob(
                    value = (masterVolume / 1.5f).coerceIn(0f, 1f),
                    onValueChange = { onMasterVolumeChange(it * 1.5f) },
                    label = "MASTER",
                    valueDisplay = "${(masterVolume * 100).toInt()}%",
                    size = 32.dp,
                    tintColor = NeonCyan
                )

                // Stereo VU Bar
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    listOf(masterPeakL, masterPeakR).forEach { peak ->
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(StudioCard, RoundedCornerShape(1.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(peak.coerceIn(0f, 1f))
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        if (peak > 0.9f) NeonRed else if (peak > 0.7f) NeonAmber else NeonGreen,
                                        RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                }
            }

            // MIDI / BLE Status Pill
            Box(
                modifier = Modifier
                    .background(
                        if (isMidiConnected) NeonGreen.copy(alpha = 0.15f) else StudioCard,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isMidiConnected) NeonGreen else StudioBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onMidiClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isMidiConnected) NeonGreen else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isMidiConnected) "MIDI ONLINE" else "MIDI / BLE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMidiConnected) NeonGreen else TextSecondary
                    )
                }
            }
        }
    }
}
