package com.gx.sleep.analysis

import com.gx.sleep.domain.model.SessionReport
import com.gx.sleep.domain.model.SoundEvent
import com.gx.sleep.domain.model.SoundEventType
import com.gx.sleep.domain.model.VolumePoint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Calculates a sleep sound score (0-100).
 * This is NOT a medical sleep quality score.
 * It reflects the noise environment during recording.
 *
 * Scoring rules:
 * - Base score: 100
 * - Higher quiet percentage -> higher score
 * - More loud events -> lower score
 * - Longer snore events -> lower score
 * - Higher baseline noise -> lower score
 */
object SleepScoreCalculator {

    fun calculate(
        totalDurationMs: Long,
        quietDurationMs: Long,
        snoreDurationMs: Long,
        eventCounts: Map<SoundEventType, Int>,
        avgDbfs: Float,
        maxDbfs: Float,
        baselineRms: Float
    ): Int {
        if (totalDurationMs <= 0) return 100

        var score = 100.0

        // Quiet percentage factor (0-30 points)
        val quietPercent = quietDurationMs.toFloat() / totalDurationMs
        score -= (1 - quietPercent) * 30

        // Snore penalty (0-25 points)
        val snoreRatio = snoreDurationMs.toDouble() / totalDurationMs
        score -= min(snoreRatio * 100, 25.0)

        // Event count penalties
        val snoreCount = eventCounts[SoundEventType.SNORE_LIKE] ?: 0
        val speechCount = eventCounts[SoundEventType.SPEECH_LIKE] ?: 0
        val coughCount = eventCounts[SoundEventType.COUGH_LIKE] ?: 0
        val impactCount = eventCounts[SoundEventType.IMPACT_NOISE] ?: 0

        score -= min(snoreCount * 1.5, 15.0)
        score -= min(speechCount * 1.0, 10.0)
        score -= min(coughCount * 0.5, 5.0)
        score -= min(impactCount * 2.0, 15.0)

        // Baseline noise penalty (0-10 points)
        // Higher baseline = noisier environment
        if (baselineRms > 100) {
            score -= min((baselineRms - 100).toDouble() * 0.05, 10.0)
        }

        // Max noise penalty (0-5 points)
        if (maxDbfs > -10f) {
            score -= 5.0
        } else if (maxDbfs > -20f) {
            score -= 2.5
        }

        return score.roundToInt().coerceIn(0, 100)
    }
}
