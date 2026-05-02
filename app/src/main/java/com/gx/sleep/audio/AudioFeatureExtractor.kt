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
 * Operates directly on a shared buffer region [offset, offset+length)
 * to avoid per-frame array allocation.
 *
 * NOTE: Band energy estimation uses a lightweight heuristic approach,
 * NOT a precise FFT-based frequency analysis. The low/mid/high ratios
 * are rough approximations suitable for basic event classification,
 * not for acoustic or medical analysis.
 */
object AudioFeatureExtractor {

    private const val MAX_AMPLITUDE = 32768f

    /**
     * Extract features from a region of a ShortArray without copying.
     */
    fun extract(buffer: ShortArray, offset: Int, length: Int): AudioFeatures {
        if (length <= 0) {
            return AudioFeatures(0f, -120f, 0f, 0f, 0f, 0f, 0f)
        }

        var sumSquares = 0.0
        var peakValue = 0
        var zeroCrossings = 0
        var prevSample = buffer[offset]

        for (i in 0 until length) {
            val s = buffer[offset + i].toInt()
            sumSquares += s.toDouble() * s.toDouble()
            val absVal = abs(s)
            if (absVal > peakValue) peakValue = absVal
            if (i > 0 && ((prevSample >= 0 && s < 0) || (prevSample < 0 && s >= 0))) {
                zeroCrossings++
            }
            prevSample = s
        }

        val rms = sqrt(sumSquares / length).toFloat()
        val dbfs = if (rms > 0) 20f * log10(rms / MAX_AMPLITUDE) else -120f
        val peak = peakValue / MAX_AMPLITUDE
        val zcr = zeroCrossings.toFloat() / length

        val bandEnergies = estimateBandEnergies(buffer, offset, length)

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

    private fun estimateBandEnergies(buffer: ShortArray, offset: Int, length: Int): FloatArray {
        if (length < 10) return floatArrayOf(0.33f, 0.33f, 0.34f)

        // Low band: energy of slow-moving average (large window)
        var lowEnergy = 0.0
        val lowWindowSize = (length / 3).coerceAtLeast(3)
        var lowSum = 0.0
        for (i in 0 until length) {
            lowSum += buffer[offset + i]
            if (i >= lowWindowSize) {
                lowSum -= buffer[offset + i - lowWindowSize]
            }
            if (i >= lowWindowSize - 1) {
                val avg = lowSum / lowWindowSize
                lowEnergy += avg * avg
            }
        }

        // High band: energy of first-difference
        var highEnergy = 0.0
        for (j in 1 until length) {
            val diff = buffer[offset + j] - buffer[offset + j - 1]
            highEnergy += diff.toDouble() * diff.toDouble()
        }
        highEnergy *= 0.25

        // Total signal energy
        var totalEnergy = 0.0
        for (i in 0 until length) {
            val s = buffer[offset + i].toDouble()
            totalEnergy += s * s
        }

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
