package com.example.soundfont

import android.content.Context
import android.net.Uri
import com.example.audio.AudioSampleData
import java.io.InputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sound bank library and factory synthesizer.
 */
class SoundBankManager(private val context: Context) {

    private val sf2Parser = SF2Parser()
    private val sfzParser = SFZParser()

    data class LoadedSoundFont(
        val id: String,
        val filename: String,
        val format: String, // "SF2", "SF3", "SFZ", "BuiltIn"
        val sampleCount: Int,
        val presets: List<String>,
        val samples: List<AudioSampleData>
    )

    private val loadedSoundFonts = mutableListOf<LoadedSoundFont>()
    private val sampleCache = mutableMapOf<String, AudioSampleData>()

    init {
        loadFactoryInstruments()
    }

    fun getAllLoadedSoundFonts(): List<LoadedSoundFont> = loadedSoundFonts

    fun getAllSamples(): List<AudioSampleData> = sampleCache.values.toList()

    fun getSample(id: String): AudioSampleData? = sampleCache[id]

    /**
     * Parse and import a SoundFont (.sf2, .sf3, or .sfz) from a Content URI.
     */
    fun importSoundFont(uri: Uri, displayName: String): Result<LoadedSoundFont> {
        return try {
            val format = when {
                displayName.endsWith(".sf2", ignoreCase = true) -> "SF2"
                displayName.endsWith(".sf3", ignoreCase = true) -> "SF3"
                displayName.endsWith(".sfz", ignoreCase = true) -> "SFZ"
                else -> "SF2"
            }

            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Não foi possível abrir o arquivo selecionado"))

            inputStream.use { stream ->
                if (format == "SFZ") {
                    val parsed = sfzParser.parse(stream, displayName)
                    val soundFont = LoadedSoundFont(
                        id = "sfz_${System.currentTimeMillis()}",
                        filename = displayName,
                        format = "SFZ",
                        sampleCount = parsed.samples.size,
                        presets = parsed.regions.mapIndexed { i, r -> "Region ${i + 1} (${r.sampleFile.ifEmpty { "Synth" }})" },
                        samples = parsed.samples
                    )
                    loadedSoundFonts.add(soundFont)
                    parsed.samples.forEach { sampleCache[it.id] = it }
                    Result.success(soundFont)
                } else {
                    // SF2 or SF3 (SF3 uses the same RIFF container format)
                    val parsed = sf2Parser.parse(stream, displayName)
                    val soundFont = LoadedSoundFont(
                        id = "sf_${System.currentTimeMillis()}",
                        filename = displayName,
                        format = format,
                        sampleCount = parsed.samples.size,
                        presets = parsed.presets.map { it.name },
                        samples = parsed.samples
                    )
                    loadedSoundFonts.add(soundFont)
                    parsed.samples.forEach { sampleCache[it.id] = it }
                    Result.success(soundFont)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Synthesizes 16 high quality studio instruments as factory presets so the app is
     * completely functional, expressive, and audible right from the first launch.
     */
    private fun loadFactoryInstruments() {
        val factorySamples = mutableListOf<AudioSampleData>()

        // 1. Acoustic Grand Piano
        factorySamples.add(createMultiHarmonicSample("piano_grand", "Grand Piano", 60, 44100, 2.5f) { t, f ->
            val fundamental = sin(2.0 * PI * f * t) * 0.7
            val h2 = sin(2.0 * PI * f * 2.0 * t) * 0.35
            val h3 = sin(2.0 * PI * f * 3.0 * t) * 0.15
            val h4 = sin(2.0 * PI * f * 4.0 * t) * 0.08
            val hammerHit = if (t < 0.02) (Random.nextDouble() - 0.5) * (1.0 - t / 0.02) * 0.25 else 0.0
            (fundamental + h2 + h3 + h4 + hammerHit) * exp(-t * 1.8)
        })

        // 2. 808 Sub Bass
        factorySamples.add(createMultiHarmonicSample("bass_808", "808 Sub Bass", 36, 44100, 2.0f) { t, f ->
            // Pitch glide down at initial punch
            val pitchEnv = 1.0 + 1.2 * exp(-t * 28.0)
            val currentF = f * pitchEnv
            val sub = sin(2.0 * PI * currentF * t)
            val saturation = sub - 0.15 * sub * sub * sub
            saturation * exp(-t * 1.4)
        })

        // 3. Electric Rhodes Tine
        factorySamples.add(createMultiHarmonicSample("rhodes_tine", "Electric Rhodes", 60, 44100, 2.8f) { t, f ->
            val tine = sin(2.0 * PI * f * 7.0 * t) * exp(-t * 15.0) * 0.4
            val bell = sin(2.0 * PI * f * 3.0 * t) * exp(-t * 5.0) * 0.3
            val body = sin(2.0 * PI * f * t) * exp(-t * 1.2) * 0.8
            (tine + bell + body)
        })

        // 4. Synth Brass
        factorySamples.add(createMultiHarmonicSample("synth_brass", "Synth Brass", 48, 44100, 2.2f) { t, f ->
            val saw1 = ((t * f) % 1.0) * 2.0 - 1.0
            val saw2 = ((t * (f * 1.004)) % 1.0) * 2.0 - 1.0
            val saw3 = ((t * (f * 0.996)) % 1.0) * 2.0 - 1.0
            val filterEnv = exp(-t * 2.0)
            (saw1 + saw2 + saw3) * 0.35 * (0.4 + 0.6 * filterEnv) * exp(-t * 0.8)
        })

        // 5. Warm Analog Pad
        factorySamples.add(createMultiHarmonicSample("warm_pad", "Warm Analog Pad", 60, 44100, 3.5f) { t, f ->
            val s1 = sin(2.0 * PI * f * t)
            val s2 = sin(2.0 * PI * (f * 1.002) * t)
            val s3 = sin(2.0 * PI * (f * 0.998) * t)
            val lfo = 1.0 + 0.1 * sin(2.0 * PI * 2.5 * t)
            (s1 + s2 + s3) * 0.33 * lfo * min(1.0, t / 0.1) * exp(-t * 0.5)
        })

        // 6. Pluck Marimba
        factorySamples.add(createMultiHarmonicSample("pluck_marimba", "Marimba Pluck", 60, 44100, 1.8f) { t, f ->
            val woodStrike = sin(2.0 * PI * f * 3.9 * t) * exp(-t * 25.0) * 0.5
            val fundamental = sin(2.0 * PI * f * t) * exp(-t * 4.5) * 0.9
            (fundamental + woodStrike)
        })

        // 7. Funk Slap Bass
        factorySamples.add(createMultiHarmonicSample("funk_bass", "Funk Slap Bass", 40, 44100, 1.6f) { t, f ->
            val slapPop = if (t < 0.015) (Random.nextDouble() - 0.5) * 0.6 else 0.0
            val saw = (((t * f) % 1.0) * 2.0 - 1.0) * 0.5
            val sub = sin(2.0 * PI * f * t) * 0.7
            (sub + saw + slapPop) * exp(-t * 2.5)
        })

        // 8. String Ensemble
        factorySamples.add(createMultiHarmonicSample("string_ensemble", "String Ensemble", 60, 44100, 3.5f) { t, f ->
            val s1 = sin(2.0 * PI * f * t)
            val s2 = sin(2.0 * PI * (f * 1.003) * t + 0.5)
            val s3 = sin(2.0 * PI * (f * 0.997) * t + 1.0)
            val s4 = sin(2.0 * PI * (f * 2.001) * t) * 0.3
            val attack = min(1.0, t / 0.25)
            (s1 + s2 + s3 + s4) * 0.3 * attack * exp(-t * 0.4)
        })

        // 9. Acid Lead
        factorySamples.add(createMultiHarmonicSample("acid_lead", "Acid Resonant Lead", 48, 44100, 1.8f) { t, f ->
            val saw = ((t * f) % 1.0) * 2.0 - 1.0
            val squ = if (((t * f) % 1.0) > 0.5) 0.5 else -0.5
            val resPeak = sin(2.0 * PI * (f * 4.0 * exp(-t * 3.0)) * t) * 0.4
            (saw * 0.6 + squ * 0.3 + resPeak) * exp(-t * 1.5)
        })

        // 10. Woodwind Flute
        factorySamples.add(createMultiHarmonicSample("flute_breath", "Woodwind Flute", 72, 44100, 2.5f) { t, f ->
            val breath = (Random.nextDouble() - 0.5) * 0.08
            val tone = sin(2.0 * PI * f * t) * 0.85 + sin(2.0 * PI * f * 2.0 * t) * 0.15
            val attack = min(1.0, t / 0.08)
            (tone + breath) * attack * exp(-t * 0.6)
        })

        // 11. 808 Kick Drum
        factorySamples.add(createPercussionSample("drum_kick_808", "808 Punch Kick", 36, 44100, 0.7f) { t ->
            val pitch = 140.0 * exp(-t * 32.0) + 45.0
            val click = if (t < 0.006) (1.0 - t / 0.006) * 0.8 else 0.0
            val sine = sin(2.0 * PI * pitch * t)
            val dist = sine - 0.2 * sine * sine * sine
            (dist + click) * exp(-t * 6.5)
        })

        // 12. Crisp Snare Drum
        factorySamples.add(createPercussionSample("drum_snare_crisp", "Crisp Snare", 38, 44100, 0.55f) { t ->
            val body = sin(2.0 * PI * (210.0 * exp(-t * 22.0) + 160.0) * t) * exp(-t * 15.0) * 0.6
            val noise = (Random.nextDouble() - 0.5) * exp(-t * 8.0) * 0.8
            (body + noise)
        })

        // 13. Closed Hi-Hat
        factorySamples.add(createPercussionSample("drum_hihat_closed", "Closed Hi-Hat", 42, 44100, 0.18f) { t ->
            val metallic = (sin(2.0 * PI * 7500.0 * t) + sin(2.0 * PI * 9200.0 * t) + sin(2.0 * PI * 11400.0 * t)) * 0.25
            val noise = (Random.nextDouble() - 0.5) * 0.75
            (metallic + noise) * exp(-t * 40.0)
        })

        // 14. Open Hi-Hat
        factorySamples.add(createPercussionSample("drum_hihat_open", "Open Hi-Hat", 46, 44100, 0.8f) { t ->
            val metallic = (sin(2.0 * PI * 6800.0 * t) + sin(2.0 * PI * 8900.0 * t) + sin(2.0 * PI * 10500.0 * t)) * 0.3
            val noise = (Random.nextDouble() - 0.5) * 0.7
            (metallic + noise) * exp(-t * 6.0)
        })

        // 15. Percussion Clap / Rimshot
        factorySamples.add(createPercussionSample("drum_clap_rim", "Studio Clap & Rim", 39, 44100, 0.45f) { t ->
            val burst1 = if (t in 0.0..0.015) (Random.nextDouble() - 0.5) * 0.6 else 0.0
            val burst2 = if (t in 0.012..0.027) (Random.nextDouble() - 0.5) * 0.8 else 0.0
            val mainClap = if (t >= 0.024) (Random.nextDouble() - 0.5) * exp(-(t - 0.024) * 14.0) * 0.9 else 0.0
            val woodTone = sin(2.0 * PI * 420.0 * t) * exp(-t * 30.0) * 0.3
            (burst1 + burst2 + mainClap + woodTone)
        })

        // 16. Crash Cymbal
        factorySamples.add(createPercussionSample("drum_crash_cymbal", "Crash Cymbal", 49, 44100, 2.2f) { t ->
            val metallic = (sin(2.0 * PI * 4200.0 * t) + sin(2.0 * PI * 6100.0 * t) + sin(2.0 * PI * 8500.0 * t) + sin(2.0 * PI * 12300.0 * t)) * 0.25
            val noise = (Random.nextDouble() - 0.5) * 0.75
            (metallic + noise) * exp(-t * 2.2)
        })

        val soundFont = LoadedSoundFont(
            id = "factory_internal",
            filename = "SF Studio Factory SoundBank",
            format = "BuiltIn",
            sampleCount = factorySamples.size,
            presets = factorySamples.map { it.name },
            samples = factorySamples
        )
        loadedSoundFonts.add(soundFont)
        factorySamples.forEach { sampleCache[it.id] = it }
    }

    private fun createMultiHarmonicSample(
        id: String,
        name: String,
        rootKey: Int,
        sampleRate: Int,
        duration: Float,
        generator: (t: Double, f: Double) -> Double
    ): AudioSampleData {
        val freq = 440.0 * Math.pow(2.0, (rootKey - 69) / 12.0)
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val sampleVal = generator(t, freq).coerceIn(-1.0, 1.0)
            buffer[i] = (sampleVal * 32000).toInt().toShort()
        }
        return AudioSampleData(
            id = id,
            name = name,
            sampleRate = sampleRate,
            rootKey = rootKey,
            samples = buffer,
            isStereo = false,
            isLooped = false,
            sourceFormat = "BuiltIn"
        )
    }

    private fun createPercussionSample(
        id: String,
        name: String,
        rootKey: Int,
        sampleRate: Int,
        duration: Float,
        generator: (t: Double) -> Double
    ): AudioSampleData {
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val sampleVal = generator(t).coerceIn(-1.0, 1.0)
            buffer[i] = (sampleVal * 32000).toInt().toShort()
        }
        return AudioSampleData(
            id = id,
            name = name,
            sampleRate = sampleRate,
            rootKey = rootKey,
            samples = buffer,
            isStereo = false,
            isLooped = false,
            sourceFormat = "BuiltIn"
        )
    }
}
