package com.gx.sleep.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.gx.sleep.debug.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Worker that encodes PCM audio to M4A (AAC) format using hardware MediaCodec.
 * Uses a single-thread channel queue to process encoding requests sequentially.
 */
class AudioEncoderWorker(private val context: Context) {

    companion object {
        private const val TAG = "AudioEncoderWorker"
        private const val AUDIO_MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC
        private const val AUDIO_BIT_RATE = 64000 // 64 kbps
        private const val AUDIO_CHANNEL_COUNT = 1 // Mono
        private const val MAX_CLIPS_PER_NIGHT = 20
    }

    data class EncodeRequest(
        val pcmData: ShortArray,
        val sampleRate: Int,
        val sessionId: Long,
        val eventId: Long,
        val eventType: String
    )

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val encodeChannel = Channel<EncodeRequest>(Channel.UNLIMITED)
    private var workerJob: Job? = null
    private var clipsSavedTonight = 0

    /**
     * Start the encoder worker.
     */
    fun start() {
        if (workerJob != null) return

        workerJob = scope.launch {
            DebugLogger.i(TAG, "AudioEncoderWorker started")
            for (request in encodeChannel) {
                try {
                    processEncodeRequest(request)
                } catch (e: Exception) {
                    Log.e(TAG, "Error encoding audio clip", e)
                    DebugLogger.e(TAG, "Error encoding audio clip: ${e.message}")
                }
            }
        }
    }

    /**
     * Stop the encoder worker.
     */
    fun stop() {
        workerJob?.cancel()
        workerJob = null
        scope.cancel()
        DebugLogger.i(TAG, "AudioEncoderWorker stopped")
    }

    /**
     * Submit an encoding request to the queue.
     * Returns true if submitted successfully, false if limit reached.
     */
    fun submitEncodeRequest(request: EncodeRequest): Boolean {
        if (clipsSavedTonight >= MAX_CLIPS_PER_NIGHT) {
            DebugLogger.w(TAG, "Max clips per night reached ($MAX_CLIPS_PER_NIGHT), skipping")
            return false
        }

        // AudioRingBuffer.getRecentSamples() already returns a fresh copy,
        // no need to copy again here
        encodeChannel.trySend(request)
        DebugLogger.d(TAG, "Encode request submitted for event ${request.eventId}")
        return true
    }

    /**
     * Process a single encode request.
     */
    private suspend fun processEncodeRequest(request: EncodeRequest) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "clip_${request.sessionId}_${request.eventId}_$timestamp.m4a"
        val outputDir = File(context.cacheDir, "audio_clips")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputFile = File(outputDir, filename)

        try {
            encodePcmToM4a(request.pcmData, request.sampleRate, outputFile)
            clipsSavedTonight++

            // Update database with the file path
            // This is done via callback to avoid direct database access from this class
            onEncodingComplete?.invoke(request.eventId, outputFile.absolutePath)

            DebugLogger.i(TAG, "Audio clip saved: ${outputFile.absolutePath} (clips tonight: $clipsSavedTonight)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encode audio clip", e)
            DebugLogger.e(TAG, "Failed to encode audio clip: ${e.message}")
            // Clean up failed file
            if (outputFile.exists()) {
                outputFile.delete()
            }
        }
    }

    /**
     * Encode PCM data to M4A (AAC) format using MediaCodec.
     */
    private fun encodePcmToM4a(pcmData: ShortArray, sampleRate: Int, outputFile: File) {
        // Create MediaFormat for AAC encoding
        val format = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, sampleRate, AUDIO_CHANNEL_COUNT).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, pcmData.size * 2) // 2 bytes per sample
        }

        // Create encoder
        val encoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        // Create muxer
        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerTrackIndex = -1
        var muxerStarted = false

        try {
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var presentationTimeUs = 0L

            // Convert Short array to Byte array (little-endian)
            val byteBuffer = ByteBuffer.allocate(pcmData.size * 2)
            byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
            for (sample in pcmData) {
                byteBuffer.putShort(sample)
            }
            val pcmBytes = byteBuffer.array()

            var inputOffset = 0

            while (!outputDone) {
                // Queue input buffers
                if (!inputDone) {
                    val inputBufferIndex = encoder.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferIndex)!!
                        val remainingBytes = pcmBytes.size - inputOffset
                        val bytesToWrite = minOf(remainingBytes, inputBuffer.remaining())

                        if (bytesToWrite > 0) {
                            inputBuffer.put(pcmBytes, inputOffset, bytesToWrite)
                            inputOffset += bytesToWrite

                            val durationUs = (bytesToWrite.toLong() / 2) * 1000000L / sampleRate
                            encoder.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                bytesToWrite,
                                presentationTimeUs,
                                0
                            )
                            presentationTimeUs += durationUs
                        } else {
                            // Signal end of stream
                            encoder.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                0,
                                presentationTimeUs,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        }
                    }
                }

                // Dequeue output buffers
                val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                when {
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // Output format changed - this is the ONLY correct time to initialize muxer
                        if (!muxerStarted) {
                            muxerTrackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                            DebugLogger.d(TAG, "Muxer started with format: ${encoder.outputFormat}")
                        }
                    }
                    outputBufferIndex >= 0 -> {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            // Codec config, ignore
                            encoder.releaseOutputBuffer(outputBufferIndex, false)
                            continue
                        }

                        if (!muxerStarted) {
                            // Should not happen if INFO_OUTPUT_FORMAT_CHANGED was handled correctly
                            Log.w(TAG, "Output buffer received before format changed, skipping")
                            encoder.releaseOutputBuffer(outputBufferIndex, false)
                            continue
                        }

                        val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)!!
                        muxer.writeSampleData(muxerTrackIndex, outputBuffer, bufferInfo)
                        encoder.releaseOutputBuffer(outputBufferIndex, false)

                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputDone = true
                        }
                    }
                }
            }
        } finally {
            try { encoder.stop() } catch (_: Exception) {}
            try { encoder.release() } catch (_: Exception) {}
            if (muxerStarted) {
                try { muxer.stop() } catch (_: Exception) {}
            }
            try { muxer.release() } catch (_: Exception) {}
        }
    }

    /**
     * Clean up old audio clips (older than 7 days).
     */
    fun cleanupOldClips() {
        val outputDir = File(context.cacheDir, "audio_clips")
        if (!outputDir.exists()) return

        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        var deletedCount = 0

        outputDir.listFiles()?.forEach { file ->
            if (file.lastModified() < sevenDaysAgo) {
                if (file.delete()) {
                    deletedCount++
                }
            }
        }

        if (deletedCount > 0) {
            DebugLogger.i(TAG, "Cleaned up $deletedCount old audio clips")
        }
    }

    /**
     * Callback for when encoding is complete.
     * Set this to update the database with the file path.
     */
    var onEncodingComplete: ((eventId: Long, filePath: String) -> Unit)? = null

    /**
     * Get the number of clips saved tonight.
     */
    fun getClipsSavedTonight(): Int = clipsSavedTonight
}