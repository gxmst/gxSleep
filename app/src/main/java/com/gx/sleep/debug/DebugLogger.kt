package com.gx.sleep.debug

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Debug logger that captures log entries for display and export.
 * Uses ArrayDeque with synchronized lock for O(1) size checking.
 * Size is tracked with AtomicInteger for O(1) access in high-frequency calls.
 */
object DebugLogger {

    private const val MAX_LOG_ENTRIES = 1000
    private const val TAG = "DebugLogger"

    private val logEntries = ArrayDeque<LogEntry>(MAX_LOG_ENTRIES)
    private val currentSize = AtomicInteger(0)
    private val isCapturing = AtomicBoolean(false)

    data class LogEntry(
        val timestamp: Long,
        val level: Level,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null
    ) {
        enum class Level {
            DEBUG, INFO, WARN, ERROR
        }

        fun format(): String {
            val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
            val time = dateFormat.format(Date(timestamp))
            val throwableStr = throwable?.let { "\n${Log.getStackTraceString(it)}" } ?: ""
            return "$time ${level.name}/$tag: $message$throwableStr"
        }
    }

    fun start() {
        if (isCapturing.compareAndSet(false, true)) {
            synchronized(logEntries) {
                logEntries.clear()
            }
            currentSize.set(0)
            Log.i(TAG, "Debug logger started")
        }
    }

    fun stop() {
        isCapturing.set(false)
        Log.i(TAG, "Debug logger stopped")
    }

    fun isCapturing(): Boolean = isCapturing.get()

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (isCapturing.get()) {
            addEntry(LogEntry.Level.DEBUG, tag, message, throwable)
        }
        Log.d(tag, message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        if (isCapturing.get()) {
            addEntry(LogEntry.Level.INFO, tag, message, throwable)
        }
        Log.i(tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (isCapturing.get()) {
            addEntry(LogEntry.Level.WARN, tag, message, throwable)
        }
        Log.w(tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isCapturing.get()) {
            addEntry(LogEntry.Level.ERROR, tag, message, throwable)
        }
        Log.e(tag, message, throwable)
    }

    private fun addEntry(level: LogEntry.Level, tag: String, message: String, throwable: Throwable?) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )
        synchronized(logEntries) {
            logEntries.addLast(entry)
            // Remove oldest entries if we exceed max size
            while (logEntries.size > MAX_LOG_ENTRIES) {
                logEntries.removeFirst()
            }
            currentSize.set(logEntries.size)
        }
    }

    fun getLogEntries(): List<LogEntry> {
        synchronized(logEntries) {
            return logEntries.toList()
        }
    }

    fun getRecentEntries(count: Int): List<LogEntry> {
        synchronized(logEntries) {
            val entries = logEntries.toList()
            return if (entries.size <= count) entries else entries.subList(entries.size - count, entries.size)
        }
    }

    fun clear() {
        synchronized(logEntries) {
            logEntries.clear()
        }
        currentSize.set(0)
    }

    /**
     * Export logs to file. This is a suspend function that runs on Dispatchers.IO
     * to avoid blocking the UI thread.
     */
    suspend fun exportToFile(context: Context): File? {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = "gxsleep_debug_$timestamp.log"
                val file = File(context.getExternalFilesDir(null), filename)

                val entriesToExport = synchronized(logEntries) {
                    logEntries.toList()
                }

                FileWriter(file).use { writer ->
                    writer.write("gxSleep Debug Log Export\n")
                    writer.write("Exported at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                    writer.write("Total entries: ${entriesToExport.size}\n")
                    writer.write("=".repeat(80) + "\n\n")

                    entriesToExport.forEach { entry ->
                        writer.write(entry.format() + "\n")
                    }
                }

                Log.i(TAG, "Log exported to: ${file.absolutePath}")
                file
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export log", e)
                null
            }
        }
    }
}