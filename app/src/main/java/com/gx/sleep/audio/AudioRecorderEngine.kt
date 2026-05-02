package com.gx.sleep.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
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
 * All audio processing happens on a dedicated background thread.
 *
 * Performance: reuses readBuffer to avoid per-frame allocation.
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
        for (rate in PREFERRED_SAMPLE_RATES) {
            val minBuf = AudioRecord.getMinBufferSize(rate, CHANNEL_CONFIG, AUDIO_FORMAT)
            if (minBuf != AudioRecord.ERROR_BAD_VALUE && minBuf != AudioRecord.ERROR) {
                try {
                    val testRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        rate,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        minBuf * 2
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

    /**
     * Start audio capture. This initializes AudioRecord and reveals the real sampleRate.
     * Must be called BEFORE createSession so the session records the actual sampleRate.
     */
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
        sampleRate = findWorkingSampleRate()

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize == AudioRecord.ERROR_BAD_VALUE || minBufferSize == AudioRecord.ERROR) {
            callback.onError(RecordingError.INIT_FAILED)
            return
        }

        bufferSize = minBufferSize * 4
        val frameSizeBytes = sampleRate * DEFAULT_FRAME_DURATION_MS / 1000 * 2

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                callback.onError(RecordingError.INIT_FAILED)
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            // P1: Reuse a single readBuffer, pass to extractor without retaining
            recordingJob = scope.launch {
                val readBuffer = ShortArray(frameSizeBytes / 2)
                Log.i(TAG, "Recording started: rate=$sampleRate, bufSize=$bufferSize, frameSize=${readBuffer.size}")

                while (isActive && isRecording) {
                    val read = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: -1
                    if (read > 0) {
                        // P1: Only copy if partial read, otherwise use readBuffer directly
                        val samples = if (read == readBuffer.size) readBuffer else readBuffer.copyOf(read)
                        val frame = AudioFrame.fromRawSamples(samples)
                        callback.onFrameCaptured(frame)
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
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        audioRecord = null
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
