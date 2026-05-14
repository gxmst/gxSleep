package com.gx.sleep.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long = 0,
    val duration: Long = 0,
    val status: SessionStatus = SessionStatus.RUNNING,
    val batteryStartPercent: Int = -1,
    val batteryEndPercent: Int = -1,
    val audioSaveMode: String = "STATS_ONLY",
    val sampleRate: Int = 16000,
    val appVersion: String = "",
    val deviceInfo: String = "",
    val notes: String = "",
    val awakeCount: Int = 0,
    val awakeDurationMs: Long = 0
)

enum class SessionStatus {
    RUNNING,
    COMPLETED,
    CRASHED,
    STOPPED_BY_SYSTEM
}
