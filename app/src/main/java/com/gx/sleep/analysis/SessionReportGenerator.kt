package com.gx.sleep.analysis

import com.gx.sleep.data.local.entity.SoundEventEntity
import com.gx.sleep.data.local.entity.SoundSampleEntity
import com.gx.sleep.domain.model.SessionReport
import com.gx.sleep.domain.model.SoundEvent
import com.gx.sleep.domain.model.SoundEventType
import com.gx.sleep.domain.model.VolumePoint

/**
 * Generates a session report from stored data.
 * Uses O(n) sliding window and bucket aggregation instead of O(n²) scans.
 */
object SessionReportGenerator {

    fun generate(
        sessionId: Long,
        startTime: Long,
        endTime: Long,
        samples: List<SoundSampleEntity>,
        events: List<SoundEventEntity>,
        baselineRms: Float
    ): SessionReport {
        val totalDurationMs = endTime - startTime

        val eventDurationMs = events.sumOf { it.durationMs }
        val quietDurationMs = (totalDurationMs - eventDurationMs).coerceAtLeast(0)

        val eventCounts = mutableMapOf<SoundEventType, Int>()
        for (type in SoundEventType.entries) { eventCounts[type] = 0 }
        var snoreDurationMs = 0L
        for (event in events) {
            val type = SoundEventType.fromString(event.type)
            eventCounts[type] = (eventCounts[type] ?: 0) + 1
            if (type == SoundEventType.SNORE_LIKE) snoreDurationMs += event.durationMs
        }

        val avgDbfs = if (samples.isNotEmpty()) samples.map { it.dbfs }.average().toFloat() else -120f
        val maxDbfs = if (samples.isNotEmpty()) samples.maxOf { it.dbfs } else -120f

        // O(n) sliding window for loudest range
        val loudestRange = findLoudestTimeRange(samples, 5 * 60 * 1000L)

        // O(n) bucket aggregation for volume curve
        val volumeCurve = buildVolumeCurve(samples, startTime, endTime)

        val domainEvents = events.map { entity ->
            SoundEvent(
                id = entity.id, sessionId = entity.sessionId,
                startTime = entity.startTime, endTime = entity.endTime,
                durationMs = entity.durationMs, type = SoundEventType.fromString(entity.type),
                confidence = entity.confidence, avgDbfs = entity.avgDbfs,
                maxDbfs = entity.maxDbfs, audioClipPath = entity.audioClipPath
            )
        }

        val sleepScore = SleepScoreCalculator.calculate(
            totalDurationMs, quietDurationMs, snoreDurationMs,
            eventCounts, avgDbfs, maxDbfs, baselineRms
        )

        return SessionReport(
            sessionId = sessionId, startTime = startTime, endTime = endTime,
            totalDurationMs = totalDurationMs, quietDurationMs = quietDurationMs,
            totalEvents = events.size,
            snoreCount = eventCounts[SoundEventType.SNORE_LIKE] ?: 0,
            speechCount = eventCounts[SoundEventType.SPEECH_LIKE] ?: 0,
            coughCount = eventCounts[SoundEventType.COUGH_LIKE] ?: 0,
            impactCount = eventCounts[SoundEventType.IMPACT_NOISE] ?: 0,
            environmentNoiseCount = eventCounts[SoundEventType.ENVIRONMENT_NOISE] ?: 0,
            loudestTimeRange = loudestRange, avgDbfs = avgDbfs, maxDbfs = maxDbfs,
            sleepScore = sleepScore, events = domainEvents, volumeCurve = volumeCurve
        )
    }

    /**
     * O(n) sliding window with running sum.
     * Samples must be sorted by timestamp ascending.
     */
    private fun findLoudestTimeRange(
        samples: List<SoundSampleEntity>,
        windowMs: Long
    ): Pair<Long, Long>? {
        if (samples.isEmpty()) return null

        val sorted = samples.sortedBy { it.timestamp }
        var maxAvg = Float.MIN_VALUE
        var bestStart = sorted.first().timestamp
        var bestEnd = sorted.first().timestamp

        var windowStart = 0
        var windowSum = 0.0
        var windowCount = 0

        for (i in sorted.indices) {
            // Add current sample to window
            windowSum += sorted[i].dbfs
            windowCount++

            // Shrink window from left if exceeds windowMs
            while (sorted[i].timestamp - sorted[windowStart].timestamp > windowMs) {
                windowSum -= sorted[windowStart].dbfs
                windowCount--
                windowStart++
            }

            val avg = (windowSum / windowCount).toFloat()
            if (avg > maxAvg) {
                maxAvg = avg
                bestStart = sorted[windowStart].timestamp
                bestEnd = sorted[i].timestamp
            }
        }

        return bestStart to bestEnd
    }

    /**
     * O(n) bucket aggregation for volume curve.
     * Groups samples into 1-minute buckets in a single pass.
     */
    private fun buildVolumeCurve(
        samples: List<SoundSampleEntity>,
        startTime: Long,
        endTime: Long
    ): List<VolumePoint> {
        if (samples.isEmpty()) return emptyList()

        val minuteMs = 60_000L
        // bucket index = (timestamp - startTime) / minuteMs
        data class Bucket(var sum: Double = 0.0, var count: Int = 0)

        val buckets = HashMap<Int, Bucket>()
        for (s in samples) {
            val idx = ((s.timestamp - startTime) / minuteMs).toInt()
            if (idx < 0) continue
            val bucket = buckets.getOrPut(idx) { Bucket() }
            bucket.sum += s.dbfs
            bucket.count++
        }

        val points = mutableListOf<VolumePoint>()
        val sortedKeys = buckets.keys.sorted()
        for (key in sortedKeys) {
            val b = buckets[key]!!
            if (b.count > 0) {
                points.add(VolumePoint(
                    timestamp = startTime + key * minuteMs,
                    dbfs = (b.sum / b.count).toFloat()
                ))
            }
        }
        return points
    }
}
