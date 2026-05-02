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
import java.util.concurrent.CopyOnWriteArrayList

class SleepRecordingService : Service() {

    companion object {
        private const val TAG = "SleepRecordingService"
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

    private val sampleBuffer = CopyOnWriteArrayList<SoundSampleEntity>()
    private val eventBuffer = CopyOnWriteArrayList<SoundEventEntity>()
    private var dbWriteCount = 0

    private var currentSessionId: Long = 0
    private var recordingStartTime: Long = 0
    private var wakeLock: PowerManager.WakeLock? = null
    private var framesInCurrentSecond = mutableListOf<AudioFrame>()
    private var currentSecondStartTime = 0L
    private var dbFlushJob: Job? = null

    @Volatile
    private var foregroundStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = RecordingNotificationManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRecording()
            return START_NOT_STICKY
        }

        // P0: Call startForeground() IMMEDIATELY before any heavy init
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
                // Cannot continue without foreground
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (!_isRecording.value) {
            startRecording()
        }
        // P0: START_NOT_STICKY prevents auto-restart after system kill
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
        // Init lightweight components
        val app = application as GxSleepApp
        repository = SleepRepository(
            app.database.sleepSessionDao(),
            app.database.soundSampleDao(),
            app.database.soundEventDao()
        )
        settingsDataStore = SettingsDataStore(this)
        debugMetrics = DebugMetricsCollector()
        audioEngine = AudioRecorderEngine(this)

        // Mark any previously running sessions as crashed
        repository.markRunningSessionsAsCrashed()

        val settings = settingsDataStore.settings.first()
        eventDetector = SoundEventDetector(sensitivity = settings.sensitivity)

        // P1: Start audio engine FIRST to get real sampleRate
        audioEngine.start(object : AudioRecorderEngine.Callback {
            override fun onFrameCaptured(frame: AudioFrame) {
                processFrame(frame)
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

        // P1: Create session with REAL sampleRate from AudioRecord
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
        _sessionId.value = sessionId
        recordingStartTime = System.currentTimeMillis()

        // Update notification to "recording" state
        notificationManager.updateToRecordingNotification()

        acquireWakeLock()
        _isRecording.value = true
        debugMetrics.onRecordingStarted(audioEngine.sampleRate, audioEngine.bufferSize)
        startDbFlushJob()

        Log.i(TAG, "Recording started, sessionId=$sessionId, sampleRate=${audioEngine.sampleRate}, bufSize=${audioEngine.bufferSize}")
    }

    private fun processFrame(frame: AudioFrame) {
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
    }

    private fun aggregateAndBufferSecond() {
        val frames = framesInCurrentSecond.toList()
        framesInCurrentSecond.clear()

        if (frames.isEmpty()) {
            currentSecondStartTime = 0L
            return
        }

        val avgRms = frames.map { it.rms }.average().toFloat()
        val avgDbfs = frames.map { it.dbfs }.average().toFloat()
        val maxPeak = frames.maxOf { it.peak }
        val avgZcr = frames.map { it.zcr }.average().toFloat()
        val avgLow = frames.map { it.lowBandEnergy }.average().toFloat()
        val avgMid = frames.map { it.midBandEnergy }.average().toFloat()
        val avgHigh = frames.map { it.highBandEnergy }.average().toFloat()

        val sample = SoundSampleEntity(
            sessionId = currentSessionId,
            timestamp = currentSecondStartTime,
            rms = avgRms,
            dbfs = avgDbfs,
            peak = maxPeak,
            zcr = avgZcr,
            lowBandEnergy = avgLow,
            midBandEnergy = avgMid,
            highBandEnergy = avgHigh
        )
        sampleBuffer.add(sample)
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
        eventBuffer.add(entity)
        _eventCount.value = eventBuffer.size
        debugMetrics.onEventDetected()
    }

    private fun startDbFlushJob() {
        dbFlushJob = scope.launch {
            while (_isRecording.value) {
                kotlinx.coroutines.delay(10_000)
                flushBuffersToDb()
            }
        }
    }

    private suspend fun flushBuffersToDb() {
        if (sampleBuffer.isNotEmpty()) {
            val toWrite = sampleBuffer.toList()
            sampleBuffer.clear()
            try {
                repository.insertSamples(toWrite)
                dbWriteCount++
                debugMetrics.onDbWrite()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write samples", e)
            }
        }
        if (eventBuffer.isNotEmpty()) {
            val toWrite = eventBuffer.toList()
            eventBuffer.clear()
            try {
                repository.insertEvents(toWrite)
                dbWriteCount++
                debugMetrics.onDbWrite()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write events", e)
            }
        }
    }

    private fun stopRecording() {
        // P2: Use a dedicated scope that won't be cancelled prematurely
        val stopScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        stopScope.launch {
            Log.i(TAG, "Stopping recording, sessionId=$currentSessionId")

            try {
                // Flush remaining event
                val lastEvent = eventDetector?.flush()
                if (lastEvent != null) {
                    handleDetectedEvent(lastEvent)
                }

                // Aggregate remaining frames
                if (framesInCurrentSecond.isNotEmpty()) {
                    aggregateAndBufferSecond()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error flushing final data", e)
            }

            // Stop audio engine (releases AudioRecord)
            try {
                audioEngine.stop()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping audio engine", e)
            }

            _isRecording.value = false

            // P2: Flush ALL remaining buffers to DB before completing session
            try {
                flushBuffersToDb()
            } catch (e: Exception) {
                Log.e(TAG, "Error flushing buffers", e)
            }

            // Complete session
            try {
                if (currentSessionId > 0) {
                    val batteryPercent = DeviceInfoProvider.getBatteryPercent(this@SleepRecordingService)
                    repository.completeSession(currentSessionId, batteryPercent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error completing session", e)
            }

            releaseWakeLock()
            debugMetrics.onRecordingStopped()

            // P2: stopForeground then stopSelf
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (_: Exception) {}
            stopSelf()

            stopScope.cancel()
            Log.i(TAG, "Recording stopped, dbWrites=$dbWriteCount")
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
            _wakeLockHeld.value = true
            Log.i(TAG, "WakeLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.i(TAG, "WakeLock released")
                }
            }
        } catch (_: Exception) {}
        wakeLock = null
        _wakeLockHeld.value = false
    }

    override fun onDestroy() {
        // P0: Mark session as crashed if still recording when service is destroyed
        if (_isRecording.value && currentSessionId > 0) {
            try {
                if (::repository.isInitialized) {
                    kotlinx.coroutines.runBlocking {
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
