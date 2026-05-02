package com.gx.sleep.audio

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

data class AudioFeatures(
    val rms: Float,
    val dbfs: Float,
    val peak: Float,
    val zcr: Float,
    val lowBandEnergy: Float,
    val midBandEnergy: Float,
    val highBandEnergy: Float
)

/**
 * Extracts audio features from raw PCM 16-bit samples.
 * All processing is done on the calling thread (should be background thread).
 * Optimized for low CPU usage.
 *
 * NOTE: Band energy estimation uses a lightweight heuristic approach,
 * NOT a precise FFT-based frequency analysis. The low/mid/high ratios
 * are rough approximations suitable for basic event classification,
 * not for acoustic or medical analysis.
 */
object AudioFeatureExtractor {

    private const val MAX_AMPLITUDE = 32768f

    fun extract(samples: ShortArray): AudioFeatures {
        if (samples.isEmpty()) {
            return AudioFeatures(0f, -120f, 0f, 0f, 0f, 0f, 0f)
        }

        var sumSquares = 0.0
        var peakValue = 0
        var zeroCrossings = 0
        var prevSample = samples[0]

        for (i in samples.indices) {
            val s = samples[i].toInt()
            sumSquares += s.toDouble() * s.toDouble()
            val absVal = abs(s)
            if (absVal > peakValue) peakValue = absVal
            if (i > 0 && ((prevSample >= 0 && s < 0) || (prevSample < 0 && s >= 0))) {
                zeroCrossings++
            }
            prevSample = s
        }

        val rms = sqrt(sumSquares / samples.size).toFloat()
        val dbfs = if (rms > 0) 20f * log10(rms / MAX_AMPLITUDE) else -120f
        val peak = peakValue / MAX_AMPLITUDE
        val zcr = zeroCrossings.toFloat() / samples.size

        val bandEnergies = estimateBandEnergies(samples)

        return AudioFeatures(
            rms = rms,
            dbfs = dbfs.coerceIn(-120f, 0f),
            peak = peak,
            zcr = zcr,
            lowBandEnergy = bandEnergies[0],
            midBandEnergy = bandEnergies[1],
            highBandEnergy = bandEnergies[2]
        )
    }

    /**
     * Lightweight 3-band energy estimation using running-average low-pass filters.
     *
     * This is a HEURISTIC approach, not a precise FFT-based frequency decomposition.
     * It uses three running averages with different window sizes to approximate
     * the energy in low, mid, and high frequency bands:
     *
     * - Low band: slow-moving average (captures low-frequency envelope)
     * - Mid band: energy in the residual after removing low and high
     * - High band: first-difference energy (captures high-frequency transients)
     *
     * The ratios are normalized to sum to 1.0 and are suitable for relative
     * comparison within a single event, not for absolute frequency measurement.
     */
    private fun estimateBandEnergies(samples: ShortArray): FloatArray {
        val n = samples.size
        if (n < 10) return floatArrayOf(0.33f, 0.33f, 0.34f)

        // Low band: energy of slow-moving average (large window)
        // Approximates content below ~300Hz at 16kHz sample rate
        var lowEnergy = 0.0
        val lowWindowSize = (n / 3).coerceAtLeast(3)
        var lowSum = 0.0
        for (i in samples.indices) {
            lowSum += samples[i]
            if (i >= lowWindowSize) {
                lowSum -= samples[i - lowWindowSize]
            }
            if (i >= lowWindowSize - 1) {
                val avg = lowSum / lowWindowSize
                lowEnergy += avg * avg
            }
        }

        // High band: energy of first-difference (adjacent sample differences)
        // Captures rapid changes / high frequency content
        var highEnergy = 0.0
        for (j in 1 until n) {
            val diff = samples[j] - samples[j - 1]
            highEnergy += diff.toDouble() * diff.toDouble()
        }
        // Scale high energy to be comparable (difference has different magnitude)
        highEnergy *= 0.25

        // Total signal energy (sum of squares)
        var totalEnergy = 0.0
        for (s in samples) {
            totalEnergy += s.toDouble() * s.toDouble()
        }

        // Mid band: residual energy not captured by low or high
        val midEnergy = (totalEnergy - lowEnergy - highEnergy).coerceAtLeast(0.0)

        val sum = lowEnergy + midEnergy + highEnergy
        return if (sum > 0) {
            floatArrayOf(
                (lowEnergy / sum).toFloat(),
                (midEnergy / sum).toFloat(),
                (highEnergy / sum).toFloat()
            )
        } else {
            floatArrayOf(0.33f, 0.33f, 0.34f)
        }
    }
}
