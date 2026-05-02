package com.gx.sleep.domain.model

data class SleepSessionSummary(
    val id: Long,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val status: String,
    val totalEvents: Int,
    val snoreCount: Int,
    val speechCount: Int,
    val coughCount: Int,
    val impactCount: Int,
    val avgDbfs: Float,
    val maxDbfs: Float,
    val quietPercent: Float,
    val sleepScore: Int
)
