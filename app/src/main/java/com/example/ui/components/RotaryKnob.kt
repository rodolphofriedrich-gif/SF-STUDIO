package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RotaryKnob(
    value: Float, // Normalized 0.0 to 1.0
    onValueChange: (Float) -> Unit,
    label: String,
    valueDisplay: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tintColor: Color = NeonCyan
) {
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Drag up to increase, down to decrease
                            dragAccumulator -= dragAmount.y * 0.006f
                            val newValue = (value + dragAccumulator).coerceIn(0f, 1f)
                            if (newValue != value) {
                                onValueChange(newValue)
                                dragAccumulator = 0f
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val strokeWidth = 3.5.dp.toPx()
                val radius = (this.size.minDimension - strokeWidth * 2) / 2
                val center = Offset(this.size.width / 2, this.size.height / 2)

                // Background track arc (from 135 deg to 405 deg = 270 deg span)
                val startAngle = 135f
                val sweepAngle = 270f
                drawArc(
                    color = StudioBorder,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active value arc
                val activeSweep = sweepAngle * value.coerceIn(0f, 1f)
                drawArc(
                    color = tintColor,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Knob center cap
                drawCircle(
                    color = StudioCard,
                    radius = radius * 0.72f,
                    center = center
                )

                // Pointer needle
                val angleRad = (startAngle + activeSweep) * (PI / 180f)
                val pointerStart = Offset(
                    center.x + (radius * 0.35f * cos(angleRad)).toFloat(),
                    center.y + (radius * 0.35f * sin(angleRad)).toFloat()
                )
                val pointerEnd = Offset(
                    center.x + (radius * 0.68f * cos(angleRad)).toFloat(),
                    center.y + (radius * 0.68f * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = tintColor,
                    start = pointerStart,
                    end = pointerEnd,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Text(
            text = label,
            fontSize = 9.sp,
            color = TextSecondary,
            maxLines = 1
        )
        Text(
            text = valueDisplay,
            fontSize = 8.sp,
            color = TextMuted,
            maxLines = 1
        )
    }
}
