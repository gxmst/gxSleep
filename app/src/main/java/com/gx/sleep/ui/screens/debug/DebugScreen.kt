package com.gx.sleep.ui.screens.debug

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.gx.sleep.debug.DebugLogger
import com.gx.sleep.service.SleepRecordingService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isRecording by SleepRecordingService.isRecording.collectAsState()
    val currentRms by SleepRecordingService.currentRms.collectAsState()
    val currentDbfs by SleepRecordingService.currentDbfs.collectAsState()
    val eventCount by SleepRecordingService.eventCount.collectAsState()
    val sessionId by SleepRecordingService.sessionId.collectAsState()
    val wakeLockHeld by SleepRecordingService.wakeLockHeld.collectAsState()
    val wakeLockEnabled by SleepRecordingService.wakeLockEnabled.collectAsState()

    var logEntries by remember { mutableStateOf(DebugLogger.getLogEntries()) }
    var isExporting by remember { mutableStateOf(false) }
    val logListState = rememberLazyListState()

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logEntries.size) {
        if (logEntries.isNotEmpty()) {
            logListState.animateScrollToItem(logEntries.size - 1)
        }
    }

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
                .padding(16.dp)
        ) {
            // Recording status card
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

            // WakeLock status card
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

            // System info card
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

            // Log viewer card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("调试日志", style = MaterialTheme.typography.titleSmall)
                        Row {
                            IconButton(
                                onClick = { logEntries = DebugLogger.getLogEntries() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "刷新日志", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    DebugLogger.clear()
                                    logEntries = emptyList()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "清空日志", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Log controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (DebugLogger.isCapturing()) {
                                    DebugLogger.stop()
                                } else {
                                    DebugLogger.start()
                                }
                                logEntries = DebugLogger.getLogEntries()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (DebugLogger.isCapturing()) "停止捕获" else "开始捕获")
                        }

                        OutlinedButton(
                            onClick = {
                                if (!isExporting) {
                                    isExporting = true
                                    scope.launch {
                                        try {
                                            val file = DebugLogger.exportToFile(context)
                                            if (file != null) {
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "分享日志文件"))
                                            } else {
                                                Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isExporting = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isExporting
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("导出中...")
                            } else {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("导出日志")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Log entries
                    if (logEntries.isEmpty()) {
                        Text(
                            text = if (DebugLogger.isCapturing()) "正在捕获日志..." else "点击\"开始捕获\"记录调试日志",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            state = logListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            items(logEntries) { entry ->
                                Text(
                                    text = entry.format(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (entry.level) {
                                        DebugLogger.LogEntry.Level.ERROR -> MaterialTheme.colorScheme.error
                                        DebugLogger.LogEntry.Level.WARN -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info card
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
