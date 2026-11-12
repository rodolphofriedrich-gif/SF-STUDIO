package com.example.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tanh

/**
 * Comb filter for algorithmic Schroeder reverberation.
 */
class CombFilter(val size: Int) {
    private val buffer = FloatArray(size)
    private var index = 0
    private var filterStore = 0.0f

    fun process(input: Float, feedback: Float, damp: Float): Float {
        val output = buffer[index]
        filterStore = (output * (1.0f - damp)) + (filterStore * damp)
        buffer[index] = input + (filterStore * feedback)
        index = (index + 1) % size
        return output
    }

    fun clear() {
        buffer.fill(0f)
        filterStore = 0f
    }
}

/**
 * All-pass filter for diffusion in reverb.
 */
class AllPassFilter(val size: Int) {
    private val buffer = FloatArray(size)
    private var index = 0

    fun process(input: Float, feedback: Float = 0.5f): Float {
        val bufOut = buffer[index]
        val output = -input + bufOut
        buffer[index] = input + (bufOut * feedback)
        index = (index + 1) % size
        return output
    }

    fun clear() {
        buffer.fill(0f)
    }
}

/**
 * Stereo Delay line with feedback and dry/wet mixing.
 */
class StereoDelay(maxDelayMs: Int = 1000, private val sampleRate: Int = 44100) {
    private val maxSamples = (sampleRate * (maxDelayMs / 1000.0)).toInt() + 10
    private val leftBuffer = FloatArray(maxSamples)
    private val rightBuffer = FloatArray(maxSamples)
    private var writeIndex = 0

    fun process(
        inL: Float,
        inR: Float,
        delayMs: Float,
        feedback: Float,
        wet: Float
    ): Pair<Float, Float> {
        if (wet <= 0.001f) return Pair(inL, inR)

        val delaySamples = min(max((sampleRate * (delayMs / 1000f)).toInt(), 1), maxSamples - 1)
        val readIdx = (writeIndex - delaySamples + maxSamples) % maxSamples

        val delayedL = leftBuffer[readIdx]
        val delayedR = rightBuffer[readIdx]

        val safeFeedback = min(feedback, 0.88f)
        leftBuffer[writeIndex] = inL + (delayedL * safeFeedback)
        rightBuffer[writeIndex] = inR + (delayedR * safeFeedback)

        writeIndex = (writeIndex + 1) % maxSamples

        val outL = inL * (1f - wet * 0.5f) + delayedL * wet
        val outR = inR * (1f - wet * 0.5f) + delayedR * wet
        return Pair(outL, outR)
    }

    fun clear() {
        leftBuffer.fill(0f)
        rightBuffer.fill(0f)
    }
}

/**
 * Algorithmic Reverb engine.
 */
class ReverbEngine(sampleRate: Int = 44100) {
    // Tuning values scaled for 44.1 kHz
    private val combL = arrayOf(
        CombFilter((1116 * sampleRate / 44100)),
        CombFilter((1188 * sampleRate / 44100)),
        CombFilter((1277 * sampleRate / 44100)),
        CombFilter((1356 * sampleRate / 44100))
    )
    private val combR = arrayOf(
        CombFilter((1139 * sampleRate / 44100)),
        CombFilter((1211 * sampleRate / 44100)),
        CombFilter((1300 * sampleRate / 44100)),
        CombFilter((1379 * sampleRate / 44100))
    )

    private val allPassL = arrayOf(
        AllPassFilter((556 * sampleRate / 44100)),
        AllPassFilter((441 * sampleRate / 44100))
    )
    private val allPassR = arrayOf(
        AllPassFilter((579 * sampleRate / 44100)),
        AllPassFilter((464 * sampleRate / 44100))
    )

    fun process(inL: Float, inR: Float, roomSize: Float, wet: Float): Pair<Float, Float> {
        if (wet <= 0.001f) return Pair(inL, inR)

        val input = (inL + inR) * 0.5f
        val feedback = 0.7f + (roomSize * 0.25f)
        val damp = 0.2f

        var outL = 0f
        var outR = 0f
        for (i in combL.indices) {
            outL += combL[i].process(input, feedback, damp)
            outR += combR[i].process(input, feedback, damp)
        }

        for (ap in allPassL) {
            outL = ap.process(outL)
        }
        for (ap in allPassR) {
            outR = ap.process(outR)
        }

        val dryL = inL * (1f - wet * 0.5f)
        val dryR = inR * (1f - wet * 0.5f)
        return Pair(dryL + outL * wet * 0.35f, dryR + outR * wet * 0.35f)
    }

    fun clear() {
        combL.forEach { it.clear() }
        combR.forEach { it.clear() }
        allPassL.forEach { it.clear() }
        allPassR.forEach { it.clear() }
    }
}

/**
 * Saturation / Tube overdrive processor.
 */
object DriveProcessor {
    fun process(sample: Float, drive: Float): Float {
        if (drive <= 0.01f) return sample
        val gain = 1.0f + drive * 5.0f
        val x = sample * gain
        // Soft clipping curve using tanh approximation
        val saturated = if (abs(x) < 1.0f) {
            x - (x * x * x / 3.0f)
        } else {
            tanh(x.toDouble()).toFloat()
        }
        return saturated / (1.0f + drive * 1.5f)
    }
}
