package com.gx.sleep.audio

/**
 * Thread-safe ring buffer for storing recent PCM audio data.
 * Used to capture audio clips around detected sound events.
 *
 * @param sampleRate Sample rate in Hz (e.g., 16000)
 * @param durationSeconds Duration to buffer in seconds (e.g., 10)
 */
class AudioRingBuffer(
    private val sampleRate: Int,
    private val durationSeconds: Int = 5
) {
    private val totalSamples = sampleRate * durationSeconds
    private val buffer = ShortArray(totalSamples)
    private var writePosition = 0
    private var samplesWritten = 0L

    /**
     * Write PCM samples to the ring buffer.
     * This method is called from the audio recording thread.
     *
     * @param samples ShortArray of PCM samples to write
     * @param offset Offset in the source array
     * @param count Number of samples to write
     */
    @Synchronized
    fun write(samples: ShortArray, offset: Int, count: Int) {
        val samplesToWrite = minOf(count, totalSamples)

        // If we're writing more than the buffer size, only keep the last portion
        val sourceOffset = if (samplesToWrite < count) offset + (count - samplesToWrite) else offset

        // Copy samples to ring buffer
        for (i in 0 until samplesToWrite) {
            buffer[writePosition] = samples[sourceOffset + i]
            writePosition = (writePosition + 1) % totalSamples
        }

        samplesWritten += count
    }

    /**
     * Get the most recent samples from the ring buffer.
     * Returns a copy of the samples in chronological order.
     *
     * @param maxSamples Maximum number of samples to return
     * @return ShortArray containing the most recent samples
     */
    @Synchronized
    fun getRecentSamples(maxSamples: Int = totalSamples): ShortArray {
        val availableSamples = minOf(samplesWritten, totalSamples.toLong()).toInt()
        val samplesToReturn = minOf(maxSamples, availableSamples)

        if (samplesToReturn == 0) return ShortArray(0)

        val result = ShortArray(samplesToReturn)

        // Calculate read position
        val readPosition = if (samplesWritten >= totalSamples) {
            // Buffer has wrapped around
            (writePosition - samplesToReturn + totalSamples) % totalSamples
        } else {
            // Buffer hasn't wrapped yet
            0
        }

        // Copy samples in chronological order
        if (readPosition + samplesToReturn <= totalSamples) {
            // No wrap around needed
            System.arraycopy(buffer, readPosition, result, 0, samplesToReturn)
        } else {
            // Wrap around
            val firstPart = totalSamples - readPosition
            System.arraycopy(buffer, readPosition, result, 0, firstPart)
            System.arraycopy(buffer, 0, result, firstPart, samplesToReturn - firstPart)
        }

        return result
    }

    /**
     * Get the number of samples currently in the buffer.
     */
    @Synchronized
    fun getAvailableSamples(): Int {
        return minOf(samplesWritten, totalSamples.toLong()).toInt()
    }

    /**
     * Clear the buffer.
     */
    @Synchronized
    fun clear() {
        writePosition = 0
        samplesWritten = 0
    }

    /**
     * Get the sample rate.
     */
    fun getSampleRate(): Int = sampleRate

    /**
     * Get the buffer duration in seconds.
     */
    fun getDurationSeconds(): Int = durationSeconds
}