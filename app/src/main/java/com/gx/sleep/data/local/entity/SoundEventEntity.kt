package com.gx.sleep.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sound_events",
    foreignKeys = [
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId", "startTime"])]
)
data class SoundEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val type: String,
    val confidence: Float,
    val avgDbfs: Float,
    val maxDbfs: Float,
    val audioClipPath: String? = null,
    val metadataJson: String = ""
)
