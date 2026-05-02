package com.gx.sleep.ui.screens.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gx.sleep.data.datastore.AudioSaveMode

@Composable
fun AudioSaveModeSelector(
    currentMode: AudioSaveMode,
    onModeSelected: (AudioSaveMode) -> Unit
) {
    Column {
        ModeOption(
            mode = AudioSaveMode.STATS_ONLY,
            label = "仅统计（当前版本唯一可用）",
            description = "不保存任何音频，只保存声音特征数据和事件统计",
            selected = currentMode == AudioSaveMode.STATS_ONLY,
            onClick = { onModeSelected(AudioSaveMode.STATS_ONLY) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        // P1: Hide EVENT_CLIPS and FULL_RECORDING - not yet implemented
        Text(
            text = "事件片段保存和完整录音将在后续版本中提供",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 48.dp)
        )
    }
}

@Composable
private fun ModeOption(
    mode: AudioSaveMode,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
