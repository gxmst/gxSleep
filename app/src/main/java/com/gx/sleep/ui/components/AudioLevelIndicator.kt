package com.gx.sleep.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.log10

@Composable
fun AudioLevelIndicator(
    rms: Float,
    modifier: Modifier = Modifier
) {
    // Convert RMS to a 0-1 visual level
    val dbfs = if (rms > 0) 20 * log10(rms / 32768f) else -120f
    val normalized = ((dbfs + 60f) / 60f).coerceIn(0f, 1f)

    val animatedLevel by animateFloatAsState(
        targetValue = normalized,
        animationSpec = tween(durationMillis = 100),
        label = "level"
    )

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier.size(width = 200.dp, height = 24.dp)) {
        val barCount = 20
        val barWidth = size.width / barCount * 0.7f
        val gap = size.width / barCount * 0.3f
        val filledBars = (animatedLevel * barCount).toInt()

        for (i in 0 until barCount) {
            val x = i * (barWidth + gap) + gap / 2
            val color = when {
                i >= filledBars -> inactiveColor
                i < barCount * 0.6f -> activeColor
                i < barCount * 0.8f -> Color(0xFFFFA726)
                else -> Color(0xFFEF5350)
            }
            drawRect(
                color = color,
                topLeft = Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(barWidth, size.height)
            )
        }
    }
}
