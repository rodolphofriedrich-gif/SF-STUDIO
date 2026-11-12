package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.audio.FilterSettings
import com.example.audio.FilterType
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.StudioBorder
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun FilterVisualizer(
    filter: FilterSettings,
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    tintColor: Color = NeonAmber
) {
    Canvas(
        modifier = modifier
            .height(height)
            .fillMaxSize()
    ) {
        val w = size.width
        val h = size.height
        val pad = 12.dp.toPx()
        val usableW = w - pad * 2
        val usableH = h - pad * 2

        // Frequency grid markers (100Hz, 1kHz, 10kHz)
        val fMin = log10(20.0)
        val fMax = log10(20000.0)
        val fSpan = fMax - fMin

        val markers = listOf(100.0, 1000.0, 10000.0)
        markers.forEach { f ->
            val normX = ((log10(f) - fMin) / fSpan).toFloat()
            val x = pad + normX * usableW
            drawLine(
                color = StudioBorder,
                start = Offset(x, pad),
                end = Offset(x, h - pad),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw frequency response curve
        val cutoffNorm = ((log10(filter.cutoffHz.toDouble().coerceIn(20.0, 20000.0)) - fMin) / fSpan).toFloat()
        val cutoffX = pad + cutoffNorm * usableW
        val qBump = (filter.resonance - 1.0f).coerceIn(0f, 6f) * (usableH * 0.08f)

        val path = Path()
        val fillPath = Path()
        val steps = 60

        for (i in 0..steps) {
            val frac = i.toFloat() / steps
            val x = pad + frac * usableW
            val y: Float = when (filter.type) {
                FilterType.LOW_PASS -> {
                    if (frac < cutoffNorm) {
                        // Pass band
                        (pad + usableH * 0.45f) - (if (cutoffNorm - frac < 0.1f) qBump else 0f)
                    } else {
                        // Roll off
                        val slope = ((frac - cutoffNorm) / (1f - cutoffNorm + 0.001f)).pow(1.8f)
                        (pad + usableH * 0.45f) + slope * usableH * 0.5f
                    }
                }
                FilterType.HIGH_PASS -> {
                    if (frac > cutoffNorm) {
                        // Pass band
                        (pad + usableH * 0.45f) - (if (frac - cutoffNorm < 0.1f) qBump else 0f)
                    } else {
                        // Roll off
                        val slope = ((cutoffNorm - frac) / (cutoffNorm + 0.001f)).pow(1.8f)
                        (pad + usableH * 0.45f) + slope * usableH * 0.5f
                    }
                }
                FilterType.BAND_PASS -> {
                    val dist = kotlin.math.abs(frac - cutoffNorm)
                    (pad + usableH * 0.45f) - qBump + dist * usableH * 1.5f
                }
            }.coerceIn(pad, h - pad)

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h - pad)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(w - pad, h - pad)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(tintColor.copy(alpha = 0.25f), Color.Transparent),
                startY = pad,
                endY = h - pad
            )
        )

        drawPath(
            path = path,
            color = tintColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Cutoff handle point
        val cutoffY = when (filter.type) {
            FilterType.LOW_PASS -> (pad + usableH * 0.45f) - qBump
            FilterType.HIGH_PASS -> (pad + usableH * 0.45f) - qBump
            FilterType.BAND_PASS -> (pad + usableH * 0.45f) - qBump
        }.coerceIn(pad, h - pad)

        drawCircle(tintColor.copy(alpha = 0.35f), radius = 7.dp.toPx(), center = Offset(cutoffX, cutoffY))
        drawCircle(Color.White, radius = 3.5.dp.toPx(), center = Offset(cutoffX, cutoffY))
    }
}
