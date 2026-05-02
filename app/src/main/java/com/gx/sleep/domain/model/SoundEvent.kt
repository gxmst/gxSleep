package com.gx.sleep.domain.model

data class SoundEvent(
    val id: Long,
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val type: SoundEventType,
    val confidence: Float,
    val avgDbfs: Float,
    val maxDbfs: Float,
    val audioClipPath: String? = null
)

enum class SoundEventType(val label: String) {
    SNORE_LIKE("疑似鼾声"),
    SPEECH_LIKE("疑似梦话"),
    COUGH_LIKE("疑似咳嗽"),
    IMPACT_NOISE("突发噪音"),
    ENVIRONMENT_NOISE("环境噪音"),
    UNKNOWN("未分类");

    companion object {
        fun fromString(value: String): SoundEventType {
            return entries.find { it.name == value } ?: UNKNOWN
        }
    }
}
