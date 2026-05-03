package com.gx.sleep.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
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

        // Draw event markers using DesignSystem EventColors
        for (event in events) {
            val startX = padding + ((event.startTime - startTime) / totalDuration) * chartWidth
            val endX = padding + ((event.endTime - startTime) / totalDuration) * chartWidth
            val color = EventColors.forType(event.type.name)

            drawRect(
                color = color.copy(alpha = 0.25f),
                topLeft = Offset(startX, padding),
                size = androidx.compose.ui.geometry.Size(
                    (endX - startX).coerceAtLeast(2f),
                    chartHeight
                )
            )
        }
    }
}
