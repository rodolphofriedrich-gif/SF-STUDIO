package com.example.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Direct Form I / II Biquad Filter for EQ and synthesis cutoff filters.
 * Runs at sample rate (default 44.1 kHz).
 */
class BiquadFilter(private val sampleRate: Float = 44100f) {
    var b0: Float = 1.0f
    var b1: Float = 0.0f
    var b2: Float = 0.0f
    var a1: Float = 0.0f
    var a2: Float = 0.0f

    var x1: Float = 0.0f
    var x2: Float = 0.0f
    var y1: Float = 0.0f
    var y2: Float = 0.0f

    fun reset() {
        x1 = 0f
        x2 = 0f
        y1 = 0f
        y2 = 0f
    }

    inline fun process(input: Float): Float {
        val out = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = input
        y2 = y1
        y1 = if (out.isNaN() || out.isInfinite()) 0f else out
        return y1
    }

    fun setLowPass(cutoffHz: Float, q: Float) {
        val f0 = min(max(20f, cutoffHz), sampleRate * 0.48f)
        val safeQ = max(0.1f, q)
        val w0 = (2.0 * PI * f0 / sampleRate).toFloat()
        val alpha = (sin(w0) / (2.0 * safeQ)).toFloat()
        val cosw0 = cos(w0)

        val a0 = 1.0f + alpha
        b0 = ((1.0f - cosw0) / 2.0f) / a0
        b1 = (1.0f - cosw0) / a0
        b2 = ((1.0f - cosw0) / 2.0f) / a0
        a1 = (-2.0f * cosw0) / a0
        a2 = (1.0f - alpha) / a0
    }

    fun setHighPass(cutoffHz: Float, q: Float) {
        val f0 = min(max(20f, cutoffHz), sampleRate * 0.48f)
        val safeQ = max(0.1f, q)
        val w0 = (2.0 * PI * f0 / sampleRate).toFloat()
        val alpha = (sin(w0) / (2.0 * safeQ)).toFloat()
        val cosw0 = cos(w0)

        val a0 = 1.0f + alpha
        b0 = ((1.0f + cosw0) / 2.0f) / a0
        b1 = (-(1.0f + cosw0)) / a0
        b2 = ((1.0f + cosw0) / 2.0f) / a0
        a1 = (-2.0f * cosw0) / a0
        a2 = (1.0f - alpha) / a0
    }

    fun setBandPass(cutoffHz: Float, q: Float) {
        val f0 = min(max(20f, cutoffHz), sampleRate * 0.48f)
        val safeQ = max(0.1f, q)
        val w0 = (2.0 * PI * f0 / sampleRate).toFloat()
        val alpha = (sin(w0) / (2.0 * safeQ)).toFloat()
        val cosw0 = cos(w0)

        val a0 = 1.0f + alpha
        b0 = alpha / a0
        b1 = 0f
        b2 = -alpha / a0
        a1 = (-2.0f * cosw0) / a0
        a2 = (1.0f - alpha) / a0
    }

    fun setLowShelf(cutoffHz: Float, gainDb: Float) {
        if (kotlin.math.abs(gainDb) < 0.1f) {
            setBypass()
            return
        }
        val f0 = min(max(20f, cutoffHz), sampleRate * 0.48f)
        val a = 10.0.pow(gainDb / 40.0).toFloat()
        val w0 = (2.0 * PI * f0 / sampleRate).toFloat()
        val cosw0 = cos(w0)
        val sinw0 = sin(w0)
        val alpha = sinw0 / 2.0f * sqrt(2.0).toFloat()
        val twoSqrtAAlpha = 2.0f * sqrt(a) * alpha

        val a0 = (a + 1.0f) + (a - 1.0f) * cosw0 + twoSqrtAAlpha
        b0 = (a * ((a + 1.0f) - (a - 1.0f) * cosw0 + twoSqrtAAlpha)) / a0
        b1 = (2.0f * a * ((a - 1.0f) - (a + 1.0f) * cosw0)) / a0
        b2 = (a * ((a + 1.0f) - (a - 1.0f) * cosw0 - twoSqrtAAlpha)) / a0
        a1 = (-2.0f * ((a - 1.0f) + (a + 1.0f) * cosw0)) / a0
        a2 = ((a + 1.0f) + (a - 1.0f) * cosw0 - twoSqrtAAlpha) / a0
    }

    fun setPeaking(freqHz: Float, gainDb: Float, q: Float) {
        if (kotlin.math.abs(gainDb) < 0.1f) {
            setBypass()
            return
        }
        val f0 = min(max(20f, freqHz), sampleRate * 0.48f)
        val safeQ = max(0.1f, q)
        val a = 10.0.pow(gainDb / 40.0).toFloat()
        val w0 = (2.0 * PI * f0 / sampleRate).toFloat()
        val cosw0 = cos(w0)
        val alpha = (sin(w0) / (2.0f * safeQ)).toFloat()

        val a0 = 1.0f + alpha / a
        b0 = (1.0f + alpha * a) / a0
        b1 = (-2.0f * cosw0) / a0
        b2 = (1.0f - alpha * a) / a0
        a1 = (-2.0f * cosw0) / a0
        a2 = (1.0f - alpha / a) / a0
    }

    fun setHighShelf(cutoffHz: Float, gainDb: Float) {
        if (kotlin.math.abs(gainDb) < 0.1f) {
            setBypass()
            return
        }
        val f0 = min(max(20f, cutoffHz), sampleRate * 0.48f)
        val a = 10.0.pow(gainDb / 40.0).toFloat()
        val w0 = (2.0 * PI * f0 / sampleRate).toFloat()
        val cosw0 = cos(w0)
        val sinw0 = sin(w0)
        val alpha = sinw0 / 2.0f * sqrt(2.0).toFloat()
        val twoSqrtAAlpha = 2.0f * sqrt(a) * alpha

        val a0 = (a + 1.0f) - (a - 1.0f) * cosw0 + twoSqrtAAlpha
        b0 = (a * ((a + 1.0f) + (a - 1.0f) * cosw0 + twoSqrtAAlpha)) / a0
        b1 = (-2.0f * a * ((a - 1.0f) + (a + 1.0f) * cosw0)) / a0
        b2 = (a * ((a + 1.0f) + (a - 1.0f) * cosw0 - twoSqrtAAlpha)) / a0
        a1 = (2.0f * ((a - 1.0f) - (a + 1.0f) * cosw0)) / a0
        a2 = ((a + 1.0f) - (a - 1.0f) * cosw0 - twoSqrtAAlpha) / a0
    }

    fun setBypass() {
        b0 = 1.0f
        b1 = 0.0f
        b2 = 0.0f
        a1 = 0.0f
        a2 = 0.0f
    }
}
