package com.example.audio

import kotlin.math.max
import kotlin.math.min

/**
 * ADSR Envelope settings in seconds and normalized levels.
 */
data class ADSR(
    val attack: Float = 0.01f,   // 0.001s to 5.0s
    val decay: Float = 0.15f,    // 0.001s to 5.0s
    val sustain: Float = 0.70f,  // 0.0 to 1.0 (level)
    val release: Float = 0.30f   // 0.01s to 10.0s
)

enum class FilterType {
    LOW_PASS,
    HIGH_PASS,
    BAND_PASS
}

/**
 * Resonant Biquad Filter settings.
 */
data class FilterSettings(
    val type: FilterType = FilterType.LOW_PASS,
    val cutoffHz: Float = 12000f,  // 20Hz to 20000Hz
    val resonance: Float = 1.0f,   // 0.1 to 10.0 (Q factor)
    val envelopeAmount: Float = 0.0f // -1.0 to +1.0
)

/**
 * 3-Band Parametric Equalizer settings.
 */
data class ParametricEQ(
    val lowShelfGainDb: Float = 0.0f,  // -12dB to +12dB (at 100Hz)
    val midFreqHz: Float = 1000f,       // 200Hz to 5000Hz
    val midGainDb: Float = 0.0f,        // -12dB to +12dB
    val midQ: Float = 1.0f,             // 0.5 to 5.0
    val highShelfGainDb: Float = 0.0f   // -12dB to +12dB (at 8000Hz)
)

/**
 * Real-time audio processing effects for each channel.
 */
data class ChannelEffects(
    val eq: ParametricEQ = ParametricEQ(),
    val drive: Float = 0.0f,           // 0.0 (clean) to 1.0 (heavy saturation)
    val delayTimeMs: Float = 250f,     // 20ms to 1000ms
    val delayFeedback: Float = 0.3f,   // 0.0 to 0.85
    val delayWet: Float = 0.0f,        // 0.0 to 1.0
    val reverbSize: Float = 0.5f,      // 0.1 to 1.0
    val reverbWet: Float = 0.0f        // 0.0 to 1.0
)

/**
 * Audio Track channel configuration (16 simultaneous tracks).
 */
data class TrackChannel(
    val id: Int,
    val name: String,
    val volume: Float = 0.8f,          // 0.0 to 1.5
    val pan: Float = 0.0f,             // -1.0 (Left) to +1.0 (Right)
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val midiChannel: Int = 0,          // 0 = Omni (all channels), 1..16 = specific channel
    val soundId: String = "synth_lead",
    val soundName: String = "Synth Lead",
    val adsr: ADSR = ADSR(),
    val filter: FilterSettings = FilterSettings(),
    val fx: ChannelEffects = ChannelEffects(),
    val transpose: Int = 0,            // -24 to +24 semitones
    val fineTuneCents: Float = 0.0f,   // -50 to +50 cents
    val chokeGroup: Int = 0            // 0 = none, 1..4 = cut group
)

/**
 * Audio sample data in memory (16-bit PCM mono or stereo).
 */
data class AudioSampleData(
    val id: String,
    val name: String,
    val sampleRate: Int = 44100,
    val rootKey: Int = 60,             // MIDI note (60 = C4)
    val samples: ShortArray,           // 16-bit PCM
    val isStereo: Boolean = false,
    val isLooped: Boolean = false,
    val loopStart: Int = 0,
    val loopEnd: Int = 0,
    val sourceFormat: String = "Internal" // "SF2", "SF3", "SFZ", "WAV", "Internal"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioSampleData
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/**
 * Envelope generator phase.
 */
enum class EnvelopeStage {
    OFF,
    ATTACK,
    DECAY,
    SUSTAIN,
    RELEASE
}

/**
 * Individual voice playing in the audio engine.
 */
class ActiveVoice(
    val trackId: Int,
    val note: Int,
    val velocity: Float,
    val sample: AudioSampleData,
    val sampleStep: Double,
    var position: Double = 0.0,
    var stage: EnvelopeStage = EnvelopeStage.ATTACK,
    var currentEnvelopeLevel: Float = 0.0f,
    var isReleased: Boolean = false,
    var ageFrames: Long = 0,
    // Per-voice filter state
    var fX1: Float = 0.0f,
    var fX2: Float = 0.0f,
    var fY1: Float = 0.0f,
    var fY2: Float = 0.0f
)
