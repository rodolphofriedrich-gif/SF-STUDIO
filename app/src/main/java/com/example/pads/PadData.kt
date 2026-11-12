package com.example.pads

import androidx.compose.ui.graphics.Color

data class PerformancePad(
    val id: Int,
    val label: String,
    val trackId: Int,
    val midiNote: Int,
    val color: Color,
    val isPressed: Boolean = false,
    val lastVelocity: Float = 1.0f
)

object DefaultPads {
    // 4x4 default layout arranged like classic MPC drum machine (Bottom row: 0, 1, 2, 3)
    val defaultList = listOf(
        // Bank 1: Drums & Percussion (Pads 0-7)
        PerformancePad(0, "808 Kick", 10, 36, Color(0xFFFF5252)),
        PerformancePad(1, "Crisp Snare", 11, 38, Color(0xFFFF7A00)),
        PerformancePad(2, "Closed Hat", 12, 42, Color(0xFFFFD600)),
        PerformancePad(3, "Open Hat", 13, 46, Color(0xFFFFEA00)),

        PerformancePad(4, "Clap & Rim", 14, 39, Color(0xFF00E676)),
        PerformancePad(5, "Crash Cymbal", 15, 49, Color(0xFF00B0FF)),
        PerformancePad(6, "808 Sub", 1, 36, Color(0xFF7C4DFF)),
        PerformancePad(7, "Funk Slap", 6, 40, Color(0xFFE040FB)),

        // Bank 2: Melodic Instruments & Synths (Pads 8-15)
        PerformancePad(8, "Grand Piano", 0, 60, Color(0xFF00E5FF)),
        PerformancePad(9, "Rhodes", 2, 64, Color(0xFF69F0AE)),
        PerformancePad(10, "Synth Brass", 3, 48, Color(0xFFFFAB40)),
        PerformancePad(11, "Analog Pad", 4, 60, Color(0xFFFF4081)),

        PerformancePad(12, "Marimba", 5, 60, Color(0xFF40C4FF)),
        PerformancePad(13, "Strings", 7, 60, Color(0xFFB388FF)),
        PerformancePad(14, "Acid Lead", 8, 48, Color(0xFFFF5252)),
        PerformancePad(15, "Wood Flute", 9, 72, Color(0xFF64FFDA))
    )
}
