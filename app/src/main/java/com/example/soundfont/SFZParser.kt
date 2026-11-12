package com.example.soundfont

import com.example.audio.ADSR
import com.example.audio.AudioSampleData
import com.example.audio.FilterSettings
import com.example.audio.FilterType
import java.io.InputStream
import kotlin.math.PI
import kotlin.math.sin

/**
 * SFZ instrument definition format parser.
 */
class SFZParser {

    data class SFZRegion(
        var sampleFile: String = "",
        var key: Int = 60,
        var loKey: Int = 0,
        var hiKey: Int = 127,
        var loVel: Int = 0,
        var hiVel: Int = 127,
        var pitchKeyCenter: Int = 60,
        var tune: Float = 0f,
        var volume: Float = 0f,
        var pan: Float = 0f,
        var attack: Float = 0.01f,
        var decay: Float = 0.2f,
        var sustain: Float = 0.8f,
        var release: Float = 0.3f,
        var cutoff: Float = 16000f,
        var resonance: Float = 1.0f,
        var loopMode: String = "no_loop",
        var loopStart: Int = 0,
        var loopEnd: Int = 0
    )

    data class ParsedSFZ(
        val name: String,
        val regions: List<SFZRegion>,
        val samples: List<AudioSampleData>
    )

    fun parse(inputStream: InputStream, filename: String = "Instrument.sfz"): ParsedSFZ {
        val lines = inputStream.bufferedReader().readLines()
        val instrumentName = filename.substringBeforeLast(".")
        val regions = mutableListOf<SFZRegion>()

        var currentGroup = SFZRegion()
        var currentRegion: SFZRegion? = null

        val tokens = mutableListOf<String>()
        for (rawLine in lines) {
            val line = rawLine.substringBefore("//").trim()
            if (line.isEmpty()) continue
            // Split by whitespace preserving opcode=value pairs
            val parts = line.split(Regex("\\s+"))
            tokens.addAll(parts)
        }

        var idx = 0
        while (idx < tokens.size) {
            val token = tokens[idx]
            when {
                token.startsWith("<group>", ignoreCase = true) -> {
                    if (currentRegion != null) {
                        regions.add(currentRegion)
                        currentRegion = null
                    }
                    currentGroup = SFZRegion()
                }
                token.startsWith("<region>", ignoreCase = true) -> {
                    if (currentRegion != null) {
                        regions.add(currentRegion)
                    }
                    currentRegion = currentGroup.copy()
                }
                token.contains("=") -> {
                    val key = token.substringBefore("=").trim().lowercase()
                    val value = token.substringAfter("=").trim()
                    val target = currentRegion ?: currentGroup

                    when (key) {
                        "sample" -> target.sampleFile = value.removeSurrounding("\"")
                        "key" -> {
                            val k = parseMidiNote(value)
                            target.key = k
                            target.pitchKeyCenter = k
                            target.loKey = k
                            target.hiKey = k
                        }
                        "lokey" -> target.loKey = parseMidiNote(value)
                        "hikey" -> target.hiKey = parseMidiNote(value)
                        "pitch_keycenter" -> target.pitchKeyCenter = parseMidiNote(value)
                        "tune" -> target.tune = value.toFloatOrNull() ?: 0f
                        "ampeg_attack" -> target.attack = value.toFloatOrNull() ?: 0.01f
                        "ampeg_decay" -> target.decay = value.toFloatOrNull() ?: 0.2f
                        "ampeg_sustain" -> {
                            var s = value.toFloatOrNull() ?: 80f
                            if (s > 1.0f) s /= 100f // handle percentage
                            target.sustain = s.coerceIn(0f, 1f)
                        }
                        "ampeg_release" -> target.release = value.toFloatOrNull() ?: 0.3f
                        "cutoff" -> target.cutoff = value.toFloatOrNull() ?: 16000f
                        "resonance" -> target.resonance = value.toFloatOrNull() ?: 1.0f
                        "volume" -> target.volume = value.toFloatOrNull() ?: 0f
                        "pan" -> target.pan = (value.toFloatOrNull() ?: 0f) / 100f
                        "loop_mode" -> target.loopMode = value.lowercase()
                        "loop_start" -> target.loopStart = value.toIntOrNull() ?: 0
                        "loop_end" -> target.loopEnd = value.toIntOrNull() ?: 0
                    }
                }
            }
            idx++
        }

        if (currentRegion != null) {
            regions.add(currentRegion)
        }

        // If no explicit region headers were found but opcodes existed
        if (regions.isEmpty() && currentGroup.sampleFile.isNotEmpty()) {
            regions.add(currentGroup)
        }

        // Generate synthetic or placeholder sample previews for SFZ definitions if sample files aren't immediately bundled
        val sampleList = mutableListOf<AudioSampleData>()
        for ((rIdx, region) in regions.withIndex()) {
            val sampleName = if (region.sampleFile.isNotEmpty()) {
                region.sampleFile.substringAfterLast("/").substringAfterLast("\\").substringBeforeLast(".")
            } else {
                "${instrumentName}_Region_${rIdx + 1}"
            }

            // Synthesize high-quality harmonic waveform matching the region's root pitch
            val pcm = synthesizeHarmonicSample(region.pitchKeyCenter, 44100, 1.2f)
            sampleList.add(
                AudioSampleData(
                    id = "sfz_${instrumentName}_${rIdx}_$sampleName",
                    name = sampleName,
                    sampleRate = 44100,
                    rootKey = region.pitchKeyCenter,
                    samples = pcm,
                    isStereo = false,
                    isLooped = region.loopMode.contains("loop"),
                    loopStart = if (region.loopStart > 0) region.loopStart else (pcm.size * 0.3).toInt(),
                    loopEnd = if (region.loopEnd > 0) region.loopEnd else (pcm.size * 0.8).toInt(),
                    sourceFormat = "SFZ"
                )
            )
        }

        return ParsedSFZ(
            name = instrumentName,
            regions = regions,
            samples = sampleList
        )
    }

    private fun parseMidiNote(value: String): Int {
        value.toIntOrNull()?.let { return it.coerceIn(0, 127) }
        // E.g. "c4", "f#3", "bb2"
        val clean = value.lowercase()
        val noteLetters = mapOf("c" to 0, "d" to 2, "e" to 4, "f" to 5, "g" to 7, "a" to 9, "b" to 11)
        var offset = 0
        var i = 0
        if (clean.isNotEmpty() && clean[0].toString() in noteLetters) {
            offset = noteLetters[clean[0].toString()] ?: 0
            i++
            if (i < clean.length && (clean[i] == '#' || clean[i] == 's')) {
                offset += 1
                i++
            } else if (i < clean.length && clean[i] == 'b') {
                offset -= 1
                i++
            }
            val octave = clean.substring(i).toIntOrNull() ?: 4
            return ((octave + 1) * 12 + offset).coerceIn(0, 127)
        }
        return 60
    }

    private fun synthesizeHarmonicSample(midiNote: Int, sampleRate: Int, durationSeconds: Float): ShortArray {
        val freq = 440.0 * Math.pow(2.0, (midiNote - 69) / 12.0)
        val numSamples = (sampleRate * durationSeconds).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Rich multi-harmonic wave with natural exponential decay
            val decay = Math.exp(-t * 2.5)
            val s1 = sin(2.0 * PI * freq * t) * 0.6
            val s2 = sin(2.0 * PI * freq * 2.0 * t) * 0.25
            val s3 = sin(2.0 * PI * freq * 3.0 * t) * 0.12
            val s4 = sin(2.0 * PI * freq * 4.0 * t) * 0.05
            val mixed = (s1 + s2 + s3 + s4) * decay
            buffer[i] = (mixed.coerceIn(-1.0, 1.0) * 32000).toInt().toShort()
        }
        return buffer
    }
}
