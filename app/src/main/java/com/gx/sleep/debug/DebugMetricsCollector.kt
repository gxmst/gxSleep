package com.gx.sleep.debug

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Collects debug metrics for the current recording session.
 * Thread-safe using atomics.
 */
class DebugMetricsCollector {

    private val framesProcessed = AtomicLong(0)
    private val samplesBuffered = AtomicLong(0)
    private val eventsDetected = AtomicLong(0)
    private val dbWrites = AtomicLong(0)
    private var recordingStartTime = 0L
    private var sampleRate = 0
    private var bufferSize = 0

    fun onRecordingStarted(rate: Int, bufSize: Int) {
        recordingStartTime = System.currentTimeMillis()
        sampleRate = rate
        bufferSize = bufSize
        framesProcessed.set(0)
        samplesBuffered.set(0)
        eventsDetected.set(0)
        dbWrites.set(0)
    }

    fun onFrameProcessed() { framesProcessed.incrementAndGet() }
    fun onSampleBuffered() { samplesBuffered.incrementAndGet() }
    fun onEventDetected() { eventsDetected.incrementAndGet() }
    fun onDbWrite() { dbWrites.incrementAndGet() }
    fun onRecordingStopped() {}

    fun getMetrics(
        sampleRate: Int,
        bufferSize: Int,
        dbWriteCount: Int,
        readErrorCount: Int,
        isInterrupted: Boolean,
        wakeLockHeld: Boolean
    ): Metrics {
        val elapsed = if (recordingStartTime > 0) {
            System.currentTimeMillis() - recordingStartTime
        } else 0

        val minutes = elapsed / 60_000.0
        val eventsPerMinute = if (minutes > 0) eventsDetected.get() / minutes else 0.0

        val runtime = Runtime.getRuntime()
        val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)

        return Metrics(
            sampleRate = sampleRate,
            bufferSize = bufferSize,
            framesProcessed = framesProcessed.get(),
            samplesBuffered = samplesBuffered.get(),
            eventsDetected = eventsDetected.get(),
            dbWriteCount = dbWriteCount,
            readErrorCount = readErrorCount,
            eventsPerMinute = eventsPerMinute,
            usedMemoryMB = usedMemoryMB,
            maxMemoryMB = maxMemoryMB,
            isInterrupted = isInterrupted,
            elapsedMinutes = minutes,
            wakeLockHeld = wakeLockHeld
        )
    }

    data class Metrics(
        val sampleRate: Int,
        val bufferSize: Int,
        val framesProcessed: Long,
        val samplesBuffered: Long,
        val eventsDetected: Long,
        val dbWriteCount: Int,
        val readErrorCount: Int,
        val eventsPerMinute: Double,
        val usedMemoryMB: Long,
        val maxMemoryMB: Long,
        val isInterrupted: Boolean,
        val elapsedMinutes: Double,
        val wakeLockHeld: Boolean
    )
}
