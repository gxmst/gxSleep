package com.gx.sleep.ui.screens.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gx.sleep.service.SleepRecordingService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(onBack: () -> Unit) {
    val isRecording by SleepRecordingService.isRecording.collectAsState()
    val currentRms by SleepRecordingService.currentRms.collectAsState()
    val currentDbfs by SleepRecordingService.currentDbfs.collectAsState()
    val eventCount by SleepRecordingService.eventCount.collectAsState()
    val sessionId by SleepRecordingService.sessionId.collectAsState()
    val wakeLockHeld by SleepRecordingService.wakeLockHeld.collectAsState()
    val wakeLockEnabled by SleepRecordingService.wakeLockEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("调试信息") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("录制状态", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    DebugRow("正在录制", if (isRecording) "是" else "否")
                    DebugRow("Session ID", "${sessionId ?: "-"}")
                    DebugRow("当前 RMS", "%.1f".format(currentRms))
                    DebugRow("当前 dBFS", "%.1f".format(currentDbfs))
                    DebugRow("已检测事件", "$eventCount")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = if (wakeLockHeld) CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) else CardDefaults.cardColors()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("WakeLock 状态", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    DebugRow("设置中已启用", if (wakeLockEnabled) "是" else "否")
                    DebugRow("当前持有", if (wakeLockHeld) "是" else "否")
                    if (wakeLockEnabled && !wakeLockHeld) {
                        Text(
                            text = "提示：已启用但未持有，可能录制尚未开始",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!wakeLockEnabled) {
                        Text(
                            text = "前台服务是默认保活机制。如锁屏录制中断，可在设置中开启实验 WakeLock。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("系统信息", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    val runtime = Runtime.getRuntime()
                    val usedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                    val maxMB = runtime.maxMemory() / (1024 * 1024)
                    DebugRow("已用内存", "${usedMB} MB")
                    DebugRow("最大内存", "${maxMB} MB")
                    DebugRow("CPU 核心", "${runtime.availableProcessors()}")
                    DebugRow("Android SDK", "${android.os.Build.VERSION.SDK_INT}")
                    DebugRow("设备", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                    DebugRow("Android 版本", android.os.Build.VERSION.RELEASE ?: "未知")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("说明", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "此页面显示调试信息，帮助排查录制问题。" +
                                "WakeLock 默认关闭，前台服务是主要保活机制。" +
                                "如果锁屏录制中断，可在设置中开启实验 WakeLock。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
