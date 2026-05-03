package com.gx.sleep.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.gx.sleep.GxSleepApp
import com.gx.sleep.analysis.DetectedEvent
import com.gx.sleep.analysis.SoundEventDetector
import com.gx.sleep.audio.AudioFrame
import com.gx.sleep.audio.AudioRecorderEngine
import com.gx.sleep.data.datastore.SettingsDataStore
import com.gx.sleep.data.local.entity.SoundEventEntity
import com.gx.sleep.data.local.entity.SoundSampleEntity
import com.gx.sleep.data.repository.SleepRepository
import com.gx.sleep.debug.DebugMetricsCollector
import com.gx.sleep.system.DeviceInfoProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex

class SleepRecordingService : Service() {

    companion object {
        private const val TAG = "SleepRecordingService"
        private const val MAX_FLUSH_RETRIES = 3
        const val ACTION_STOP = RecordingNotificationManager.ACTION_STOP
        const val ACTION_START = "com.gx.sleep.ACTION_START_RECORDING"

        private val _isRecording = MutableStateFlow(false)
        val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

        private val _currentRms = MutableStateFlow(0f)
        val currentRms: StateFlow<Float> = _currentRms.asStateFlow()

        private val _currentDbfs = MutableStateFlow(-120f)
        val currentDbfs: StateFlow<Float> = _currentDbfs.asStateFlow()

        private val _sessionId = MutableStateFlow<Long?>(null)
        val sessionId: StateFlow<Long?> = _sessionId.asStateFlow()

        private val _error = MutableSharedFlow<ErrorInfo>()
        val error: SharedFlow<ErrorInfo> = _error.asSharedFlow()

        private val _eventCount = MutableStateFlow(0)
        val eventCount: StateFlow<Int> = _eventCount.asStateFlow()

        private val _wakeLockHeld = MutableStateFlow(false)
        val wakeLockHeld: StateFlow<Boolean> = _wakeLockHeld.asStateFlow()

        private val _wakeLockEnabled = MutableStateFlow(false)
        val wakeLockEnabled: StateFlow<Boolean> = _wakeLockEnabled.asStateFlow()

        // P1: Emitted after session is fully flushed and completed in DB.
        // UI should navigate to report only after this event, not on isRecording=false.
        private val _recordingCompleted = MutableSharedFlow<Long>(extraBufferCapacity = 1)
        val recordingCompleted: SharedFlow<Long> = _recordingCompleted.asSharedFlow()

        // P2: Exposed so UI can disable stop button during stop sequence
        private val _isStopping = MutableStateFlow(false)
        val isStopping: StateFlow<Boolean> = _isStopping.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, SleepRecordingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SleepRecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    data class ErrorInfo(
        val error: AudioRecorderEngine.RecordingError,
        val message: String
    )

    private lateinit var audioEngine: AudioRecorderEngine
    private lateinit var notificationManager: RecordingNotificationManager
    private lateinit var repository: SleepRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var debugMetrics: DebugMetricsCollector

    private var eventDetector: SoundEventDetector? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val stopScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // P1: Mutex serializes processFrame and final flush to prevent concurrent access
    private val frameMutex = Mutex()

    private val sampleBufferLock = Any()
    private val sampleBuffer = ArrayList<SoundSampleEntity>(256)
    private val eventBufferLock = Any()
    private val eventBuffer = ArrayList<SoundEventEntity>(32)
    private var dbWriteCount = 0

    // P2: Session-level event counter that never resets during a session
    private var sessionEventCount = 0

    private var currentSessionId: Long = 0
    private var recordingStartTime: Long = 0
    private var wakeLock: PowerManager.WakeLock? = null
    private var wakeLockAcquireTime: Long = 0
    private val framesInCurrentSecond = ArrayList<AudioFrame>(25)
    private var currentSecondStartTime = 0L
    private var dbFlushJob: Job? = null

    @Volatile
    private var foregroundStarted = false

    // P1: Gate to block processFrame during stop sequence
    @Volatile
    private var acceptingFrames = false

    // P2: Idempotency guard for stopRecording
    @Volatile
    private var isStopping = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = RecordingNotificationManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            if (!isStopping) {
                stopRecording()
            }
            return START_NOT_STICKY
        }

        if (!foregroundStarted) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        RecordingNotificationManager.NOTIFICATION_ID,
                        notificationManager.buildInitNotification(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(
                        RecordingNotificationManager.NOTIFICATION_ID,
                        notificationManager.buildInitNotification()
                    )
                }
                foregroundStarted = true
            } catch (e: Exception) {
                Log.e(TAG, "startForeground failed", e)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (!_isRecording.value) {
            startRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        scope.launch {
            try {
                initializeRecording()
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException during init", e)
                scope.launch { _error.emit(ErrorInfo(AudioRecorderEngine.RecordingError.PERMISSION_DENIED, "权限被拒绝: ${e.message}")) }
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "Exception during recording init", e)
                scope.launch { _error.emit(ErrorInfo(AudioRecorderEngine.RecordingError.INIT_FAILED, "初始化失败: ${e.message}")) }
                stopSelf()
            }
        }
    }

    private suspend fun initializeRecording() {
        val app = application as GxSleepApp
        repository = SleepRepository(
            app.database.sleepSessionDao(),
            app.database.soundSampleDao(),
            app.database.soundEventDao()
        )
        settingsDataStore = SettingsDataStore(this)
        debugMetrics = DebugMetricsCollector()
        audioEngine = AudioRecorderEngine(this)

        repository.markRunningSessionsAsCrashed()

        val settings = settingsDataStore.settings.first()
        eventDetector = SoundEventDetector(sensitivity = settings.sensitivity)

        _wakeLockEnabled.value = settings.enableWakeLock

        audioEngine.start(object : AudioRecorderEngine.Callback {
            override fun onFrameCaptured(frame: AudioFrame) {
                if (acceptingFrames) {
                    processFrame(frame)
                }
            }

            override fun onError(error: AudioRecorderEngine.RecordingError) {
                val msg = when (error) {
                    AudioRecorderEngine.RecordingError.PERMISSION_DENIED -> "麦克风权限被撤销"
                    AudioRecorderEngine.RecordingError.INIT_FAILED -> "麦克风初始化失败"
                    AudioRecorderEngine.RecordingError.RECORD_FAILED -> "录音读取失败"
                    AudioRecorderEngine.RecordingError.DEVICE_UNAVAILABLE -> "音频设备不可用"
                    AudioRecorderEngine.RecordingError.MIC_IN_USE -> "麦克风被其他应用占用"
                }
                scope.launch { _error.emit(ErrorInfo(error, msg)) }
                if (error == AudioRecorderEngine.RecordingError.PERMISSION_DENIED) {
                    stopRecording()
                }
            }
        })

        if (!audioEngine.isRecording) {
            Log.e(TAG, "AudioEngine failed to start")
            _error.emit(ErrorInfo(AudioRecorderEngine.RecordingError.INIT_FAILED, "录音引擎启动失败"))
            stopSelf()
            return
        }

        val batteryPercent = DeviceInfoProvider.getBatteryPercent(this@SleepRecordingService)
        val deviceInfo = DeviceInfoProvider.getDeviceInfo()
        val sessionId = repository.createSession(
            sampleRate = audioEngine.sampleRate,
            audioSaveMode = settings.audioSaveMode.value,
            batteryPercent = batteryPercent,
            deviceInfo = deviceInfo,
            appVersion = DeviceInfoProvider.getAppVersionName(this@SleepRecordingService)
        )

        currentSessionId = sessionId
        sessionEventCount = 0
        _sessionId.value = sessionId
        _eventCount.value = 0
        recordingStartTime = System.currentTimeMillis()

        notificationManager.updateToRecordingNotification()

        if (settings.enableWakeLock) {
            acquireWakeLock()
        }

        acceptingFrames = true
        _isRecording.value = true
        debugMetrics.onRecordingStarted(audioEngine.sampleRate, audioEngine.bufferSize)
        startDbFlushJob()

        Log.i(TAG, "Recording started, sessionId=$sessionId, sampleRate=${audioEngine.sampleRate}, wakeLock=${settings.enableWakeLock}")
    }

    private fun processFrame(frame: AudioFrame) {
        // P1: Mutex ensures final flush doesn't race with in-flight callbacks.
        // tryLock means if stop is flushing, we silently drop this frame (acceptable).
        if (!frameMutex.tryLock()) return
        try {
            _currentRms.value = frame.rms
            _currentDbfs.value = frame.dbfs
            debugMetrics.onFrameProcessed()

            if (currentSecondStartTime == 0L) {
                currentSecondStartTime = frame.timestamp
            }
            framesInCurrentSecond.add(frame)

            if (frame.timestamp - currentSecondStartTime >= 1000) {
                aggregateAndBufferSecond()
            }

            val event = eventDetector?.feedFrame(frame)
            if (event != null) {
                handleDetectedEvent(event)
            }
        } finally {
            frameMutex.unlock()
        }
    }

    private fun aggregateAndBufferSecond() {
        val count = framesInCurrentSecond.size
        if (count == 0) {
            currentSecondStartTime = 0L
            return
        }

        var sumRms = 0f; var sumDbfs = 0f; var maxPeak = 0f
        var sumZcr = 0f; var sumLow = 0f; var sumMid = 0f; var sumHigh = 0f
        for (f in framesInCurrentSecond) {
            sumRms += f.rms; sumDbfs += f.dbfs
            if (f.peak > maxPeak) maxPeak = f.peak
            sumZcr += f.zcr; sumLow += f.lowBandEnergy
            sumMid += f.midBandEnergy; sumHigh += f.highBandEnergy
        }
        framesInCurrentSecond.clear()

        val sample = SoundSampleEntity(
            sessionId = currentSessionId,
            timestamp = currentSecondStartTime,
            rms = sumRms / count,
            dbfs = sumDbfs / count,
            peak = maxPeak,
            zcr = sumZcr / count,
            lowBandEnergy = sumLow / count,
            midBandEnergy = sumMid / count,
            highBandEnergy = sumHigh / count
        )

        synchronized(sampleBufferLock) { sampleBuffer.add(sample) }
        debugMetrics.onSampleBuffered()
        currentSecondStartTime = 0L
    }

    private fun handleDetectedEvent(event: DetectedEvent) {
        val entity = SoundEventEntity(
            sessionId = currentSessionId,
            startTime = event.startTime,
            endTime = event.endTime,
            durationMs = event.durationMs,
            type = event.type.name,
            confidence = event.confidence,
            avgDbfs = event.avgDbfs,
            maxDbfs = event.maxDbfs,
            audioClipPath = null
        )
        synchronized(eventBufferLock) { eventBuffer.add(entity) }
        // P2: Increment session-level counter, never reset during session
        sessionEventCount++
        _eventCount.value = sessionEventCount
        debugMetrics.onEventDetected()
    }

    private fun startDbFlushJob() {
        dbFlushJob = scope.launch {
            while (_isRecording.value) {
                kotlinx.coroutines.delay(10_000)
                try { flushBuffersToDb() } catch (_: Exception) {}
            }
        }
    }

    /**
     * P1: Clear-after-success with retry.
     * Returns true if all batches were successfully written, false if any failed.
     */
    private suspend fun flushBuffersToDb(): Boolean {
        var allSuccess = true

        // Snapshot samples
        val samplesToWrite: List<SoundSampleEntity> = synchronized(sampleBufferLock) {
            if (sampleBuffer.isEmpty()) return@synchronized emptyList()
            val copy = sampleBuffer.toList()
            sampleBuffer.clear()
            copy
        }
        if (samplesToWrite.isNotEmpty()) {
            var written = false
            for (attempt in 1..MAX_FLUSH_RETRIES) {
                try {
                    repository.insertSamples(samplesToWrite)
                    dbWriteCount++
                    debugMetrics.onDbWrite()
                    written = true
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write samples (attempt $attempt/$MAX_FLUSH_RETRIES)", e)
                    if (attempt < MAX_FLUSH_RETRIES) {
                        kotlinx.coroutines.delay(1000L * attempt)
                    }
                }
            }
            if (!written) {
                Log.w(TAG, "Requeuing ${samplesToWrite.size} samples after $MAX_FLUSH_RETRIES failures")
                synchronized(sampleBufferLock) {
                    sampleBuffer.addAll(0, samplesToWrite)
                }
                allSuccess = false
            }
        }

        // Snapshot events
        val eventsToWrite: List<SoundEventEntity> = synchronized(eventBufferLock) {
            if (eventBuffer.isEmpty()) return@synchronized emptyList()
            val copy = eventBuffer.toList()
            eventBuffer.clear()
            copy
        }
        if (eventsToWrite.isNotEmpty()) {
            var written = false
            for (attempt in 1..MAX_FLUSH_RETRIES) {
                try {
                    repository.insertEvents(eventsToWrite)
                    dbWriteCount++
                    debugMetrics.onDbWrite()
                    written = true
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write events (attempt $attempt/$MAX_FLUSH_RETRIES)", e)
                    if (attempt < MAX_FLUSH_RETRIES) {
                        kotlinx.coroutines.delay(1000L * attempt)
                    }
                }
            }
            if (!written) {
                Log.w(TAG, "Requeuing ${eventsToWrite.size} events after $MAX_FLUSH_RETRIES failures")
                synchronized(eventBufferLock) {
                    eventBuffer.addAll(0, eventsToWrite)
                }
                allSuccess = false
            }
        }

        return allSuccess
    }

    /**
     * P2: Idempotent stop — second call is a no-op.
     * P1: Uses Mutex to wait for any in-flight processFrame to finish before flushing.
     * P1: Only emits completion if flush AND completeSession both succeed.
     */
    private fun stopRecording() {
        // P2: Idempotency guard
        if (isStopping) {
            Log.w(TAG, "stopRecording already in progress, ignoring duplicate call")
            return
        }
        isStopping = true
        _isStopping.value = true

        val capturedSessionId = currentSessionId
        stopScope.launch {
            Log.i(TAG, "Stopping recording, sessionId=$capturedSessionId")

            // Step 1: Block new frames
            acceptingFrames = false

            // Step 2: Stop audio engine — cancels recording job
            try { audioEngine.stop() } catch (e: Exception) {
                Log.e(TAG, "Error stopping audio engine", e)
            }

            // Step 3: Acquire mutex — waits for any in-flight processFrame to finish
            frameMutex.lock()
            try {
                // Now safe to flush (no concurrent processFrame)
                val lastEvent = eventDetector?.flush()
                if (lastEvent != null) handleDetectedEvent(lastEvent)
                if (framesInCurrentSecond.isNotEmpty()) aggregateAndBufferSecond()
            } finally {
                frameMutex.unlock()
            }

            // Step 4: Flush buffers to DB
            val flushOk = try { flushBuffersToDb() } catch (e: Exception) {
                Log.e(TAG, "Error flushing buffers", e)
                false
            }

            // Step 5: Complete session
            var completeOk = false
            if (flushOk) {
                try {
                    if (capturedSessionId > 0) {
                        val batteryPercent = DeviceInfoProvider.getBatteryPercent(this@SleepRecordingService)
                        repository.completeSession(capturedSessionId, batteryPercent)
                        completeOk = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error completing session", e)
                }
            }

            // Step 6: Signal result
            _isRecording.value = false
            if (flushOk && completeOk) {
                _recordingCompleted.tryEmit(capturedSessionId)
            } else {
                // Mark as crashed — data is incomplete
                try { repository.markSessionCrashed(capturedSessionId) } catch (_: Exception) {}
                Log.w(TAG, "Session marked CRASHED: flushOk=$flushOk, completeOk=$completeOk")
            }

            releaseWakeLock()
            debugMetrics.onRecordingStopped()

            try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Exception) {}

            if (flushOk && completeOk) {
                try {
                    notificationManager.sendCompletionNotification(capturedSessionId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending completion notification", e)
                }
            }

            stopSelf()
            isStopping = false
            _isStopping.value = false
            Log.i(TAG, "Recording stopped, flushOk=$flushOk, completeOk=$completeOk, dbWrites=$dbWriteCount")
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "gxSleep::RecordingWakeLock"
            ).apply {
                acquire(8 * 60 * 60 * 1000L)
            }
            wakeLockAcquireTime = System.currentTimeMillis()
            _wakeLockHeld.value = true
            Log.i(TAG, "WakeLock acquired (experimental, enabled by user)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.i(TAG, "WakeLock released, held for ${(System.currentTimeMillis() - wakeLockAcquireTime) / 1000}s")
                }
            }
        } catch (_: Exception) {}
        wakeLock = null
        _wakeLockHeld.value = false
    }

    override fun onDestroy() {
        if (_isRecording.value && currentSessionId > 0) {
            try {
                if (::repository.isInitialized) {
                    runBlocking {
                        repository.markSessionCrashed(currentSessionId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark session crashed in onDestroy", e)
            }
        }
        try { if (::audioEngine.isInitialized) audioEngine.release() } catch (_: Exception) {}
        try { scope.cancel() } catch (_: Exception) {}
        releaseWakeLock()
        super.onDestroy()
    }

    fun getDebugMetrics(): DebugMetricsCollector.Metrics? {
        if (!::audioEngine.isInitialized || !::debugMetrics.isInitialized) return null
        return debugMetrics.getMetrics(
            sampleRate = audioEngine.sampleRate,
            bufferSize = audioEngine.bufferSize,
            dbWriteCount = dbWriteCount,
            readErrorCount = audioEngine.getReadErrorCount(),
            isInterrupted = audioEngine.hasError,
            wakeLockHeld = _wakeLockHeld.value
        )
    }
}
