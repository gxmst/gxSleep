package com.gx.sleep.audio

/**
 * Lightweight audio frame containing only extracted features.
 * No raw samples stored to minimize GC pressure.
 * If audio clip saving is needed in the future, use a separate AudioRingBuffer.
 */
data class AudioFrame(
    val timestamp: Long,
    val rms: Float,
    val dbfs: Float,
    val peak: Float,
    val zcr: Float,
    val lowBandEnergy: Float,
    val midBandEnergy: Float,
    val highBandEnergy: Float
) {
    companion object {
        /**
         * Extract features from raw samples and return a lightweight frame.
         * The samples array is NOT retained.
         */
        fun fromRawSamples(
            samples: ShortArray,
            timestamp: Long = System.currentTimeMillis()
        ): AudioFrame {
            val features = AudioFeatureExtractor.extract(samples)
            return AudioFrame(
                timestamp = timestamp,
                rms = features.rms,
                dbfs = features.dbfs,
                peak = features.peak,
                zcr = features.zcr,
                lowBandEnergy = features.lowBandEnergy,
                midBandEnergy = features.midBandEnergy,
                highBandEnergy = features.highBandEnergy
            )
        }
    }
}
