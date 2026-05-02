package com.gx.sleep.audio

/**
 * Lightweight audio frame containing only extracted features.
 * No raw samples stored to minimize GC pressure.
 * Features are extracted directly from the AudioRecord readBuffer
 * without any array copy.
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
         * Extract features directly from a shared read buffer region.
         * No array copy occurs. The buffer is NOT retained.
         */
        fun fromBuffer(
            buffer: ShortArray,
            offset: Int,
            length: Int,
            timestamp: Long = System.currentTimeMillis()
        ): AudioFrame {
            val features = AudioFeatureExtractor.extract(buffer, offset, length)
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
