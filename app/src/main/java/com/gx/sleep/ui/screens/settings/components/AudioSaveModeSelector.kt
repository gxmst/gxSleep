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
            label = "仅保存统计数据",
            description = "不保存录音，只保存声音特征和事件统计（推荐）",
            selected = currentMode == AudioSaveMode.STATS_ONLY,
            onClick = { onModeSelected(AudioSaveMode.STATS_ONLY) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "事件片段保存将在后续版本中提供",
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
