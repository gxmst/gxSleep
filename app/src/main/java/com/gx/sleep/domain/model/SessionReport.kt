package com.gx.sleep.domain.model

data class SessionReport(
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long,
    val totalDurationMs: Long,
    val quietDurationMs: Long,
    val totalEvents: Int,
    val snoreCount: Int,
    val speechCount: Int,
    val coughCount: Int,
    val impactCount: Int,
    val environmentNoiseCount: Int,
    val loudestTimeRange: Pair<Long, Long>?,
    val avgDbfs: Float,
    val maxDbfs: Float,
    val sleepScore: Int,
    val events: List<SoundEvent>,
    val volumeCurve: List<VolumePoint>
) {
    val quietPercent: Float
        get() = if (totalDurationMs > 0) quietDurationMs.toFloat() / totalDurationMs * 100f else 100f
    val formattedDuration: String
        get() {
            val hours = totalDurationMs / 3600000
            val minutes = (totalDurationMs % 3600000) / 60000
            return "${hours}h ${minutes}m"
        }
}

data class VolumePoint(
    val timestamp: Long,
    val dbfs: Float
)
