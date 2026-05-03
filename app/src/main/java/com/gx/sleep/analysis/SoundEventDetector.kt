package com.gx.sleep.analysis

import com.gx.sleep.audio.AudioFrame
import com.gx.sleep.domain.model.SoundEventType

/**
 * Detects sound events from a stream of audio frames.
 * Uses a dynamic threshold based on environmental noise baseline.
 * Events are detected when amplitude exceeds baseline + threshold.
 */
class SoundEventDetector(
    private val classifier: SoundEventClassifier = RuleBasedSoundEventClassifier(),
    private val sensitivity: Int = 50
) {
    companion object {
        private const val BASELINE_WINDOW_SECONDS = 30
        private const val EVENT_MIN_DURATION_MS = 200
        private const val SILENCE_TIMEOUT_MS = 800
    }

    private var baselineRms: Float = 50f
        private set
    private var baselineInitialized = false
    private val baselineSamples = ArrayDeque<Float>()

    private var eventStartTime: Long = 0
    private var eventFrames = mutableListOf<AudioFrame>()
    private var inEvent = false

    private val thresholdFactor: Float
        get() = 1.5f + (100 - sensitivity) * 0.05f

    private var totalFramesProcessed = 0L
    private var eventsDetected = 0L

    fun feedFrame(frame: AudioFrame): DetectedEvent? {
        totalFramesProcessed++

        // P2: Only update baseline when NOT inside an event.
        // This prevents sustained loud sounds (snoring, speech) from
        // raising the noise floor and causing event fragmentation.
        if (!inEvent) {
            updateBaseline(frame)
        }

        val threshold = baselineRms * thresholdFactor
        val isSoundLoud = frame.rms > threshold

        if (isSoundLoud) {
            if (!inEvent) {
                inEvent = true
                eventStartTime = frame.timestamp
                eventFrames.clear()
            }
            eventFrames.add(frame)
            return null
        } else if (inEvent) {
            eventFrames.add(frame)
            val silenceDuration = frame.timestamp - (eventFrames.lastOrNull { it.rms > threshold }?.timestamp ?: eventStartTime)
            if (silenceDuration > SILENCE_TIMEOUT_MS) {
                return finalizeEvent()
            }
        }
        return null
    }

    fun flush(): DetectedEvent? {
        if (inEvent && eventFrames.isNotEmpty()) {
            return finalizeEvent()
        }
        return null
    }

    private fun finalizeEvent(): DetectedEvent? {
        inEvent = false

        val threshold = baselineRms * thresholdFactor
        val loudFrames = eventFrames.filter { it.rms > threshold }
        if (loudFrames.isEmpty()) {
            eventFrames.clear()
            return null
        }

        val eventEnd = loudFrames.last().timestamp
        val durationMs = eventEnd - eventStartTime

        if (durationMs < EVENT_MIN_DURATION_MS) {
            eventFrames.clear()
            return null
        }

        val avgRms = loudFrames.map { it.rms }.average().toFloat()
        val maxRms = loudFrames.maxOf { it.rms }
        val avgZcr = loudFrames.map { it.zcr }.average().toFloat()
        val avgDbfs = loudFrames.map { it.dbfs }.average().toFloat()
        val maxDbfs = loudFrames.maxOf { it.dbfs }
        val avgLow = loudFrames.map { it.lowBandEnergy }.average().toFloat()
        val avgMid = loudFrames.map { it.midBandEnergy }.average().toFloat()
        val avgHigh = loudFrames.map { it.highBandEnergy }.average().toFloat()

        val roughness = calculateRmsEnvelopeRoughness(loudFrames)

        val (type, confidence) = classifier.classify(
            durationMs = durationMs,
            avgRms = avgRms,
            maxRms = maxRms,
            avgZcr = avgZcr,
            avgDbfs = avgDbfs,
            maxDbfs = maxDbfs,
            lowBandRatio = avgLow,
            midBandRatio = avgMid,
            highBandRatio = avgHigh,
            rmsEnvelopeRoughness = roughness
        )

        eventsDetected++

        val result = DetectedEvent(
            startTime = eventStartTime,
            endTime = eventEnd,
            durationMs = durationMs,
            type = type,
            confidence = confidence,
            avgDbfs = avgDbfs,
            maxDbfs = maxDbfs
        )

        eventFrames.clear()
        return result
    }

    private fun updateBaseline(frame: AudioFrame) {
        if (baselineSamples.size < BASELINE_WINDOW_SECONDS * 20) {
            baselineSamples.addLast(frame.rms)
        } else {
            baselineSamples.removeFirst()
            baselineSamples.addLast(frame.rms)
        }

        if (!baselineInitialized && baselineSamples.size >= 60) {
            baselineRms = percentile(baselineSamples.toList(), 0.25f)
            baselineInitialized = true
        } else if (baselineInitialized) {
            val newBaseline = percentile(baselineSamples.toList(), 0.25f)
            baselineRms = baselineRms * 0.98f + newBaseline * 0.02f
        }
    }

    private fun percentile(values: List<Float>, p: Float): Float {
        if (values.isEmpty()) return 0f
        val sorted = values.sorted()
        val index = ((sorted.size - 1) * p).toInt().coerceIn(0, sorted.size - 1)
        return sorted[index]
    }

    private fun calculateRmsEnvelopeRoughness(frames: List<AudioFrame>): Float {
        if (frames.size < 4) return 0f
        val rmsValues = frames.map { it.rms }
        val mean = rmsValues.average().toFloat()
        if (mean < 0.01f) return 0f

        var sumProduct = 0f
        var sumSq = 0f
        for (i in 1 until rmsValues.size) {
            val d1 = rmsValues[i] - mean
            val d2 = rmsValues[i - 1] - mean
            sumProduct += d1 * d2
            sumSq += d1 * d1
        }
        return if (sumSq > 0) (sumProduct / sumSq).coerceIn(0f, 1f) else 0f
    }

    fun getBaselineRms(): Float = baselineRms
    fun getTotalFramesProcessed(): Long = totalFramesProcessed
    fun getEventsDetected(): Long = eventsDetected
}

/**
 * Lightweight detected event. No raw frame data stored.
 * Audio clip saving will use a separate buffer when implemented.
 */
data class DetectedEvent(
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val type: SoundEventType,
    val confidence: Float,
    val avgDbfs: Float,
    val maxDbfs: Float
)
