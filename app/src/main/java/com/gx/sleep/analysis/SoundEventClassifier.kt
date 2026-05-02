package com.gx.sleep.analysis

import com.gx.sleep.domain.model.SoundEventType

/**
 * Interface for sound event classification.
 * First version uses rule-based classification.
 * Future versions can implement with on-device ML models (TFLite/ONNX).
 */
interface SoundEventClassifier {
    /**
     * Classify a sound event based on its features.
     * @param durationMs Duration of the sound event in milliseconds
     * @param avgRms Average RMS of the event frames
     * @param maxRms Maximum RMS of the event frames
     * @param avgZcr Average zero-crossing rate
     * @param avgDbfs Average dBFS
     * @param maxDbfs Maximum dBFS
     * @param lowBandRatio Ratio of low-frequency energy
     * @param midBandRatio Ratio of mid-frequency energy
     * @param highBandRatio Ratio of high-frequency energy
     * @param rmsEnvelopeRoughness Roughness of the RMS envelope (periodicity indicator)
     * @return Pair of SoundEventType and confidence (0.0 - 1.0)
     */
    fun classify(
        durationMs: Long,
        avgRms: Float,
        maxRms: Float,
        avgZcr: Float,
        avgDbfs: Float,
        maxDbfs: Float,
        lowBandRatio: Float,
        midBandRatio: Float,
        highBandRatio: Float,
        rmsEnvelopeRoughness: Float
    ): Pair<SoundEventType, Float>
}

/**
 * Rule-based sound event classifier.
 *
 * Classification heuristics:
 * - SNORE_LIKE: long duration, low-mid frequency, periodic RMS envelope, low ZCR
 * - SPEECH_LIKE: medium duration, mid-high frequency, higher ZCR
 * - COUGH_LIKE: short burst, wide frequency, sharp attack
 * - IMPACT_NOISE: very short, very loud, sharp attack
 * - ENVIRONMENT_NOISE: long, steady, low amplitude
 * - UNKNOWN: doesn't match other patterns
 */
class RuleBasedSoundEventClassifier : SoundEventClassifier {

    override fun classify(
        durationMs: Long,
        avgRms: Float,
        maxRms: Float,
        avgZcr: Float,
        avgDbfs: Float,
        maxDbfs: Float,
        lowBandRatio: Float,
        midBandRatio: Float,
        highBandRatio: Float,
        rmsEnvelopeRoughness: Float
    ): Pair<SoundEventType, Float> {
        // IMPACT_NOISE: very short and very loud
        if (durationMs < 500 && maxDbfs > -20f) {
            return SoundEventType.IMPACT_NOISE to 0.75f
        }

        // COUGH_LIKE: short burst, wide spectrum, sharp attack
        if (durationMs in 100..1200 && maxDbfs > -25f && highBandRatio > 0.2f) {
            return SoundEventType.COUGH_LIKE to 0.65f
        }

        // SNORE_LIKE: longer, low frequency dominated, periodic
        if (durationMs > 500 && lowBandRatio > 0.4f && avgZcr < 0.15f && rmsEnvelopeRoughness > 0.3f) {
            val confidence = when {
                durationMs > 2000 && lowBandRatio > 0.6f -> 0.8f
                durationMs > 1000 -> 0.65f
                else -> 0.5f
            }
            return SoundEventType.SNORE_LIKE to confidence
        }

        // SPEECH_LIKE: medium duration, higher ZCR, mid-high frequency
        if (durationMs in 300..5000 && avgZcr > 0.08f && (midBandRatio > 0.2f || highBandRatio > 0.15f)) {
            return SoundEventType.SPEECH_LIKE to 0.6f
        }

        // ENVIRONMENT_NOISE: long steady sound
        if (durationMs > 3000 && avgDbfs < -30f) {
            return SoundEventType.ENVIRONMENT_NOISE to 0.7f
        }

        // Default
        return SoundEventType.UNKNOWN to 0.3f
    }
}
