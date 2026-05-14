package com.gx.sleep.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.gx.sleep.debug.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Core audio capture engine using AudioRecord.
 * Captures PCM 16-bit mono audio and dispatches frames via callback.
 *
 * Performance: uses a single readBuffer for the lifetime of the recording.
 * AudioFrame.fromBuffer() extracts features in-place without copying.
 *
 * Audio focus: Uses AudioRecordingCallback (API 24+) to detect when microphone
 * is taken by system (e.g., phone call). Does NOT request audio focus for playback
 * to avoid interrupting user's music/white-noise apps.
 */
class AudioRecorderEngine(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorderEngine"
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val PREFERRED_SAMPLE_RATES = intArrayOf(16000, 8000, 44100)
        const val DEFAULT_FRAME_DURATION_MS = 50
    }

    interface Callback {
        fun onFrameCaptured(frame: AudioFrame)
        fun onPcmDataAvailable(pcmData: ShortArray, offset: Int, length: Int)
        fun onError(error: RecordingError)
    }

    enum class RecordingError {
        PERMISSION_DENIED,
        INIT_FAILED,
        RECORD_FAILED,
        DEVICE_UNAVAILABLE,
        MIC_IN_USE
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Volatile
    private var isMicCapturedBySystem = false

    /**
     * API 24+: Detects when microphone is captured by system (phone call, other app).
     * This does NOT affect playback audio - user's music/white-noise continues playing.
     */
    private val recordingCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: MutableList<android.media.AudioRecordingConfiguration>?) {
                // Check if our recording is still active
                val ourRecordActive = configs?.any { config ->
                    audioRecord?.let { record ->
                        config.clientAudioSessionId == record.audioSessionId
                    } ?: false
                } ?: false

                if (isRecording && !ourRecordActive && audioRecord != null) {
                    // Our recording was taken by system
                    Log.w(TAG, "Microphone captured by system (phone call or other app)")
                    DebugLogger.w(TAG, "Microphone captured by system")
                    isMicCapturedBySystem = true
                    // Stop the hardware to prevent buffer overflow
                    try {
                        audioRecord?.stop()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping AudioRecord on mic capture", e)
                    }
                } else if (isMicCapturedBySystem && ourRecordActive) {
                    // We got the microphone back
                    Log.i(TAG, "Microphone returned to our app")
                    DebugLogger.i(TAG, "Microphone returned to our app")
                    isMicCapturedBySystem = false
                    // Restart recording
                    try {
                        audioRecord?.startRecording()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restarting AudioRecord", e)
                        DebugLogger.e(TAG, "Error restarting AudioRecord", e)
                    }
                }
            }
        }
    } else {
        null
    }

    var sampleRate: Int = 0
        private set
    var bufferSize: Int = 0
        private set
    var isRecording: Boolean = false
        private set
    var hasError: Boolean = false
        private set

    private var readErrorCount = 0

    fun getReadErrorCount(): Int = readErrorCount

    private fun findWorkingSampleRate(): Int {
        // Permission check: this method is only called from start() which already checks permission
        // but we add an explicit check here to satisfy lint
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return 16000 // Default rate, will fail later in start() with proper error
        }

        for (rate in PREFERRED_SAMPLE_RATES) {
            val minBuf = AudioRecord.getMinBufferSize(rate, CHANNEL_CONFIG, AUDIO_FORMAT)
            if (minBuf != AudioRecord.ERROR_BAD_VALUE && minBuf != AudioRecord.ERROR) {
                try {
                    val testRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC, rate,
                        CHANNEL_CONFIG, AUDIO_FORMAT, minBuf * 2
                    )
                    if (testRecord.state == AudioRecord.STATE_INITIALIZED) {
                        testRecord.release()
                        return rate
                    }
                    testRecord.release()
                } catch (_: Exception) {}
            }
        }
        return 16000
    }

    fun start(callback: Callback) {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            callback.onError(RecordingError.PERMISSION_DENIED)
            return
        }

        readErrorCount = 0
        hasError = false
        isMicCapturedBySystem = false
        sampleRate = findWorkingSampleRate()

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize == AudioRecord.ERROR_BAD_VALUE || minBufferSize == AudioRecord.ERROR) {
            callback.onError(RecordingError.INIT_FAILED)
            return
        }

        bufferSize = minBufferSize * 4
        val frameSizeSamples = sampleRate * DEFAULT_FRAME_DURATION_MS / 1000

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                callback.onError(RecordingError.INIT_FAILED)
                return
            }

            // Register recording callback to detect mic capture by system (API 24+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                recordingCallback?.let {
                    audioManager.registerAudioRecordingCallback(it, null)
                }
            }

            audioRecord?.startRecording()
            isRecording = true

            // Single readBuffer reused for entire recording lifetime.
            // Read into buffer, then extract features in-place via offset+length.
            // No per-frame ShortArray allocation.
            recordingJob = scope.launch {
                val readBuffer = ShortArray(frameSizeSamples)
                Log.i(TAG, "Recording started: rate=$sampleRate, bufSize=$bufferSize, frameSamples=$frameSizeSamples")
                DebugLogger.i(TAG, "Recording started: rate=$sampleRate, bufSize=$bufferSize")

                while (isActive && isRecording) {
                    // If microphone is captured by system, wait and retry
                    if (isMicCapturedBySystem) {
                        kotlinx.coroutines.delay(500)
                        // Try to restart if mic is available again
                        if (!isMicCapturedBySystem && isRecording) {
                            try {
                                audioRecord?.startRecording()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error restarting recording", e)
                            }
                        }
                        continue
                    }

                    val read = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: -1
                    if (read > 0) {
                        // Zero-copy: pass buffer + offset + length
                        val frame = AudioFrame.fromBuffer(readBuffer, 0, read)
                        callback.onFrameCaptured(frame)
                        // Pass raw PCM data for audio clip recording
                        callback.onPcmDataAvailable(readBuffer, 0, read)
                    } else if (read < 0) {
                        readErrorCount++
                        Log.e(TAG, "AudioRecord read error: $read, count=$readErrorCount")
                        if (readErrorCount > 50) {
                            hasError = true
                            callback.onError(RecordingError.RECORD_FAILED)
                            break
                        }
                    }
                }
                Log.i(TAG, "Recording loop ended")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            releaseAudioRecord()
            callback.onError(RecordingError.INIT_FAILED)
        }
    }

    fun stop() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        releaseAudioRecord()
        Log.i(TAG, "Recording stopped")
    }

    private fun releaseAudioRecord() {
        // Unregister recording callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recordingCallback?.let {
                try {
                    audioManager.unregisterAudioRecordingCallback(it)
                } catch (_: Exception) {}
            }
        }
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        audioRecord = null
    }

    fun release() {
        stop()
        scope.cancel()
    }
}