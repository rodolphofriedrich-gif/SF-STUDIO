package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioHighlight
import com.example.ui.theme.TextMuted

@Composable
fun MixerFader(
    volume: Float, // 0.0 to 1.5
    peakLevel: Float, // 0.0 to 1.0
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 130.dp
) {
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier.height(height)
    ) {
        // 1. Fader Track & Thumb
        Box(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Drag up to increase volume, down to decrease
                            dragAccumulator -= dragAmount.y * (1.5f / size.height)
                            val newVol = (volume + dragAccumulator).coerceIn(0f, 1.5f)
                            if (newVol != volume) {
                                onVolumeChange(newVol)
                                dragAccumulator = 0f
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackWidth = 4.dp.toPx()
                val trackX = (size.width - trackWidth) / 2
                val thumbWidth = 24.dp.toPx()
                val thumbHeight = 16.dp.toPx()

                // Background track slot
                drawRoundRect(
                    color = StudioBorder,
                    topLeft = Offset(trackX, 0f),
                    size = Size(trackWidth, size.height),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                // Zero dB mark (at 1.0 volume)
                val zeroDbY = size.height * (1.0f - (1.0f / 1.5f))
                drawLine(
                    color = TextMuted,
                    start = Offset(0f, zeroDbY),
                    end = Offset(size.width, zeroDbY),
                    strokeWidth = 1.dp.toPx()
                )

                // Fader thumb position
                val normalizedVol = (volume / 1.5f).coerceIn(0f, 1f)
                val thumbY = (size.height - thumbHeight) * (1.0f - normalizedVol)

                // Thumb body
                drawRoundRect(
                    color = StudioHighlight,
                    topLeft = Offset((size.width - thumbWidth) / 2, thumbY),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                // Thumb white center line
                drawLine(
                    color = Color.White,
                    start = Offset((size.width - thumbWidth) / 2 + 2.dp.toPx(), thumbY + thumbHeight / 2),
                    end = Offset((size.width + thumbWidth) / 2 - 2.dp.toPx(), thumbY + thumbHeight / 2),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }

        // 2. LED Segment VU Peak Meter
        Box(
            modifier = Modifier
                .width(8.dp)
                .fillMaxHeight()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val segments = 12
                val spacing = 2.dp.toPx()
                val segHeight = (size.height - (segments - 1) * spacing) / segments
                val currentNormPeak = peakLevel.coerceIn(0f, 1f)
                val activeSegments = (currentNormPeak * segments).toInt()

                for (i in 0 until segments) {
                    val segIndexFromBottom = i
                    val segY = size.height - (i + 1) * segHeight - i * spacing
                    val isActive = segIndexFromBottom < activeSegments

                    val segColor = when {
                        i >= segments - 2 -> NeonRed
                        i >= segments - 4 -> Color(0xFFFFB300)
                        else -> NeonGreen
                    }

                    drawRoundRect(
                        color = if (isActive) segColor else StudioCard,
                        topLeft = Offset(0f, segY),
                        size = Size(size.width, segHeight),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )
                }
            }
        }
    }
}
