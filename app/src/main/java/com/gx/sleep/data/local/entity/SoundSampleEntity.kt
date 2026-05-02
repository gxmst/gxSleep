package com.gx.sleep.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sound_samples",
    foreignKeys = [
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId", "timestamp"], unique = true)]
)
data class SoundSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val rms: Float,
    val dbfs: Float,
    val peak: Float,
    val zcr: Float,
    val lowBandEnergy: Float = 0f,
    val midBandEnergy: Float = 0f,
    val highBandEnergy: Float = 0f
)
