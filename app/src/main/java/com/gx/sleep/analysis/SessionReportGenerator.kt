package com.gx.sleep.analysis

import com.gx.sleep.data.local.entity.SoundEventEntity
import com.gx.sleep.data.local.entity.SoundSampleEntity
import com.gx.sleep.domain.model.SessionReport
import com.gx.sleep.domain.model.SoundEvent
import com.gx.sleep.domain.model.SoundEventType
import com.gx.sleep.domain.model.VolumePoint

/**
 * Generates a session report from stored data.
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

        // Calculate quiet duration (time not covered by events)
        val eventDurationMs = events.sumOf { it.durationMs }
        val quietDurationMs = (totalDurationMs - eventDurationMs).coerceAtLeast(0)

        // Count events by type
        val eventCounts = mutableMapOf<SoundEventType, Int>()
        for (type in SoundEventType.entries) {
            eventCounts[type] = 0
        }
        var snoreDurationMs = 0L
        for (event in events) {
            val type = SoundEventType.fromString(event.type)
            eventCounts[type] = (eventCounts[type] ?: 0) + 1
            if (type == SoundEventType.SNORE_LIKE) {
                snoreDurationMs += event.durationMs
            }
        }

        // Calculate average and max dBFS from samples
        val avgDbfs = if (samples.isNotEmpty()) samples.map { it.dbfs }.average().toFloat() else -120f
        val maxDbfs = if (samples.isNotEmpty()) samples.maxOf { it.dbfs } else -120f

        // Find loudest time range (5-minute window)
        val loudestRange = findLoudestTimeRange(samples, 5 * 60 * 1000L)

        // Volume curve (one point per minute)
        val volumeCurve = buildVolumeCurve(samples, startTime, endTime)

        // Convert entities to domain models
        val domainEvents = events.map { entity ->
            SoundEvent(
                id = entity.id,
                sessionId = entity.sessionId,
                startTime = entity.startTime,
                endTime = entity.endTime,
                durationMs = entity.durationMs,
                type = SoundEventType.fromString(entity.type),
                confidence = entity.confidence,
                avgDbfs = entity.avgDbfs,
                maxDbfs = entity.maxDbfs,
                audioClipPath = entity.audioClipPath
            )
        }

        val sleepScore = SleepScoreCalculator.calculate(
            totalDurationMs = totalDurationMs,
            quietDurationMs = quietDurationMs,
            snoreDurationMs = snoreDurationMs,
            eventCounts = eventCounts,
            avgDbfs = avgDbfs,
            maxDbfs = maxDbfs,
            baselineRms = baselineRms
        )

        return SessionReport(
            sessionId = sessionId,
            startTime = startTime,
            endTime = endTime,
            totalDurationMs = totalDurationMs,
            quietDurationMs = quietDurationMs,
            totalEvents = events.size,
            snoreCount = eventCounts[SoundEventType.SNORE_LIKE] ?: 0,
            speechCount = eventCounts[SoundEventType.SPEECH_LIKE] ?: 0,
            coughCount = eventCounts[SoundEventType.COUGH_LIKE] ?: 0,
            impactCount = eventCounts[SoundEventType.IMPACT_NOISE] ?: 0,
            environmentNoiseCount = eventCounts[SoundEventType.ENVIRONMENT_NOISE] ?: 0,
            loudestTimeRange = loudestRange,
            avgDbfs = avgDbfs,
            maxDbfs = maxDbfs,
            sleepScore = sleepScore,
            events = domainEvents,
            volumeCurve = volumeCurve
        )
    }

    private fun findLoudestTimeRange(
        samples: List<SoundSampleEntity>,
        windowMs: Long
    ): Pair<Long, Long>? {
        if (samples.isEmpty()) return null

        val sortedSamples = samples.sortedBy { it.timestamp }
        var maxAvg = Float.MIN_VALUE
        var bestStart = sortedSamples.first().timestamp
        var bestEnd = sortedSamples.first().timestamp

        var windowStart = 0
        for (i in sortedSamples.indices) {
            val windowEnd = sortedSamples[i].timestamp
            while (windowEnd - sortedSamples[windowStart].timestamp > windowMs) {
                windowStart++
            }
            val windowSamples = sortedSamples.subList(windowStart, i + 1)
            val avg = windowSamples.map { it.dbfs }.average().toFloat()
            if (avg > maxAvg) {
                maxAvg = avg
                bestStart = sortedSamples[windowStart].timestamp
                bestEnd = windowEnd
            }
        }

        return bestStart to bestEnd
    }

    private fun buildVolumeCurve(
        samples: List<SoundSampleEntity>,
        startTime: Long,
        endTime: Long
    ): List<VolumePoint> {
        if (samples.isEmpty()) return emptyList()

        val minuteMs = 60_000L
        val points = mutableListOf<VolumePoint>()
        var t = startTime

        while (t < endTime) {
            val windowEnd = t + minuteMs
            val windowSamples = samples.filter { it.timestamp in t until windowEnd }
            if (windowSamples.isNotEmpty()) {
                val avgDbfs = windowSamples.map { it.dbfs }.average().toFloat()
                points.add(VolumePoint(timestamp = t, dbfs = avgDbfs))
            }
            t = windowEnd
        }

        return points
    }
}
