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
import com.example.audio.ADSR
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard

@Composable
fun ADSRVisualizer(
    adsr: ADSR,
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    tintColor: Color = NeonCyan
) {
    Canvas(
        modifier = modifier
            .height(height)
            .fillMaxSize()
    ) {
        val w = size.width
        val h = size.height
        val pad = 12.dp.toPx()

        // Background grid lines
        drawLine(StudioBorder, Offset(pad, pad), Offset(pad, h - pad), 1.dp.toPx())
        drawLine(StudioBorder, Offset(pad, h - pad), Offset(w - pad, h - pad), 1.dp.toPx())

        // Calculate proportional widths
        // Total time domain represented: ~2.5 seconds total
        val attackNorm = (adsr.attack / 2.0f).coerceIn(0.04f, 0.35f)
        val decayNorm = (adsr.decay / 2.0f).coerceIn(0.04f, 0.35f)
        val releaseNorm = (adsr.release / 4.0f).coerceIn(0.04f, 0.35f)
        val sustainHoldNorm = (1.0f - attackNorm - decayNorm - releaseNorm).coerceIn(0.15f, 0.5f)

        val totalNorm = attackNorm + decayNorm + sustainHoldNorm + releaseNorm
        val usableWidth = w - pad * 2
        val usableHeight = h - pad * 2

        val p0 = Offset(pad, h - pad)
        val p1 = Offset(pad + (attackNorm / totalNorm) * usableWidth, pad) // Attack peak
        val sustainY = (h - pad) - adsr.sustain.coerceIn(0f, 1f) * usableHeight
        val p2 = Offset(p1.x + (decayNorm / totalNorm) * usableWidth, sustainY) // Decay end
        val p3 = Offset(p2.x + (sustainHoldNorm / totalNorm) * usableWidth, sustainY) // Sustain end
        val p4 = Offset(w - pad, h - pad) // Release end

        // Gradient Fill Path
        val fillPath = Path().apply {
            moveTo(p0.x, p0.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            lineTo(p4.x, p4.y)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(tintColor.copy(alpha = 0.3f), Color.Transparent),
                startY = pad,
                endY = h - pad
            )
        )

        // Envelope Stroke Path
        val strokePath = Path().apply {
            moveTo(p0.x, p0.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            lineTo(p4.x, p4.y)
        }

        drawPath(
            path = strokePath,
            color = tintColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Glowing nodes
        listOf(p1, p2, p3).forEach { pt ->
            drawCircle(tintColor.copy(alpha = 0.3f), radius = 6.dp.toPx(), center = pt)
            drawCircle(Color.White, radius = 3.dp.toPx(), center = pt)
        }
    }
}
