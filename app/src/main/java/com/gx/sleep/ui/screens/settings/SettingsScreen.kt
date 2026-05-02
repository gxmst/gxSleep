package com.gx.sleep.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gx.sleep.data.datastore.AppSettings
import com.gx.sleep.data.datastore.SettingsDataStore
import com.gx.sleep.ui.screens.settings.components.AudioSaveModeSelector
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBatteryGuide: () -> Unit,
    onPrivacy: () -> Unit,
    onDebug: () -> Unit
) {
    val context = LocalContext.current
    val settingsDataStore = remember { SettingsDataStore(context) }
    val settings by settingsDataStore.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 24.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "调整录制参数和应用选项",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Audio save mode
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("录音保存方式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(12.dp))
                AudioSaveModeSelector(
                    currentMode = settings.audioSaveMode,
                    onModeSelected = { mode ->
                        scope.launch { settingsDataStore.updateAudioSaveMode(mode) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sensitivity
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("检测灵敏度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "灵敏度越高，越容易检测到较轻的声音",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Slider(
                    value = settings.sensitivity.toFloat(),
                    onValueChange = { value ->
                        scope.launch { settingsDataStore.updateSensitivity(value.toInt()) }
                    },
                    valueRange = 10f..90f,
                    steps = 7
                )
                Text(
                    text = when {
                        settings.sensitivity < 30 -> "低灵敏度"
                        settings.sensitivity < 70 -> "中等灵敏度"
                        else -> "高灵敏度"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // WakeLock
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("实验 WakeLock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(
                        text = "默认关闭。仅在锁屏录制中断时开启",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.enableWakeLock,
                    onCheckedChange = { enabled ->
                        scope.launch { settingsDataStore.updateWakeLock(enabled) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation items
        SettingNavItem("电池优化建议", onBatteryGuide)
        Spacer(modifier = Modifier.height(8.dp))
        SettingNavItem("隐私说明", onPrivacy)
        Spacer(modifier = Modifier.height(8.dp))
        SettingNavItem("调试信息", onDebug)
    }
}

@Composable
private fun SettingNavItem(label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
