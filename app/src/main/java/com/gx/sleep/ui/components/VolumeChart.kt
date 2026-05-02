package com.gx.sleep.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gx.sleep.domain.model.SoundEventType
import com.gx.sleep.domain.model.SoundEvent
import com.gx.sleep.domain.model.VolumePoint

@Composable
fun VolumeChart(
    volumeCurve: List<VolumePoint>,
    events: List<SoundEvent>,
    startTime: Long,
    endTime: Long,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val eventColors = mapOf(
        SoundEventType.SNORE_LIKE to Color(0xFFFF9800),
        SoundEventType.SPEECH_LIKE to Color(0xFF4CAF50),
        SoundEventType.COUGH_LIKE to Color(0xFFF44336),
        SoundEventType.IMPACT_NOISE to Color(0xFFE91E63),
        SoundEventType.ENVIRONMENT_NOISE to Color(0xFF9E9E9E),
        SoundEventType.UNKNOWN to Color(0xFF607D8B)
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        if (volumeCurve.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val padding = 40f
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        val totalDuration = (endTime - startTime).toFloat()
        if (totalDuration <= 0) return@Canvas

        // dBFS range: -120 to 0
        val minDb = -80f
        val maxDb = -10f

        // Draw volume line
        val path = Path()
        var first = true
        for (point in volumeCurve) {
            val x = padding + ((point.timestamp - startTime) / totalDuration) * chartWidth
            val normalized = ((point.dbfs - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)
            val y = padding + chartHeight * (1f - normalized)

            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(path, lineColor, style = Stroke(width = 2f))

        // Draw event markers
        for (event in events) {
            val startX = padding + ((event.startTime - startTime) / totalDuration) * chartWidth
            val endX = padding + ((event.endTime - startTime) / totalDuration) * chartWidth
            val color = eventColors[event.type] ?: Color.Gray

            drawRect(
                color = color.copy(alpha = 0.3f),
                topLeft = Offset(startX, padding),
                size = androidx.compose.ui.geometry.Size(
                    (endX - startX).coerceAtLeast(2f),
                    chartHeight
                )
            )
        }
    }
}
