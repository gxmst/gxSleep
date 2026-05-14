package com.gx.sleep.ui.screens.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gx.sleep.ui.components.EventTypeChip
import com.gx.sleep.ui.components.SectionHeader
import com.gx.sleep.ui.components.SleepCard
import com.gx.sleep.ui.components.SleepDimens
import com.gx.sleep.ui.components.StatusBadge

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onNavigateToRecording: () -> Unit,
    onNavigateToSessionDetail: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else true
        viewModel.updatePermissionState(audioGranted, notifGranted)
    }

    LaunchedEffect(Unit) {
        val audioGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        viewModel.updatePermissionState(audioGranted, notifGranted)
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SleepDimens.screenPaddingH)
                .padding(top = SleepDimens.screenPaddingTop, bottom = SleepDimens.screenPaddingBottom),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.NightsStay,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "今晚好好睡",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "手机放在床边，锁屏后继续记录夜间声音",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(SleepDimens.sectionGap))

            if (state.lastSessionCrashed) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(SleepDimens.cardRadius)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("上次记录意外中断", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("数据已自动保存", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        TextButton(onClick = { viewModel.dismissCrashedWarning() }) {
                            Text("知道了")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(SleepDimens.itemGap))
            }

            val needsAudio = !state.hasAudioPermission
            val needsNotif = !state.hasNotificationPermission
            if (needsAudio || needsNotif) {
                SleepCard {
                    SectionHeader(title = "需要授权", subtitle = "以下权限用于睡眠记录功能")
                    Spacer(modifier = Modifier.height(12.dp))
                    if (needsAudio) {
                        PermissionRow(Icons.Outlined.Mic, "麦克风权限", "采集环境声音", false)
                    }
                    if (needsNotif) {
                        PermissionRow(Icons.Outlined.Notifications, "通知权限", "显示录制状态", false)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val perms = mutableListOf<String>()
                            if (needsAudio) perms.add(Manifest.permission.RECORD_AUDIO)
                            if (needsNotif && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            if (perms.isNotEmpty()) permissionLauncher.launch(perms.toTypedArray())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("授权权限")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("前往系统设置")
                    }
                }
                Spacer(modifier = Modifier.height(SleepDimens.sectionGap))
            }

            if (state.isRecording) {
                RecordingOrb(
                    currentRms = state.currentRms,
                    eventCount = state.eventCount,
                    onNavigateToRecording = onNavigateToRecording
                )
            } else {
                IdleOrb(
                    onNavigateToRecording = {
                        if (state.hasAudioPermission && state.hasNotificationPermission) {
                            viewModel.startRecording()
                            onNavigateToRecording()
                        } else {
                            val perms = mutableListOf<String>()
                            if (!state.hasAudioPermission) perms.add(Manifest.permission.RECORD_AUDIO)
                            if (!state.hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            if (perms.isNotEmpty()) permissionLauncher.launch(perms.toTypedArray())
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(SleepDimens.sectionGap))

            state.lastReport?.let { report ->
                SectionHeader(title = "昨晚摘要")
                Spacer(modifier = Modifier.height(SleepDimens.itemGap))
                SleepCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("睡眠声音评分", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${report.sleepScore}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        val quietDesc = when {
                            report.quietPercent > 90 -> "很安静"
                            report.quietPercent > 70 -> "较安静"
                            report.quietPercent > 50 -> "有声音"
                            else -> "较嘈杂"
                        }
                        StatusBadge(
                            text = quietDesc,
                            color = if (report.quietPercent > 70) Color(0xFF60C080) else Color(0xFFE8A040)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatItemMini("时长", report.formattedDuration, Modifier.weight(1f))
                        StatItemMini("声音", "${report.totalEvents} 段", Modifier.weight(1f))
                        StatItemMini("安静", "${report.quietPercent.toInt()}%", Modifier.weight(1f))
                    }

                    if (report.snoreCount > 0 || report.speechCount > 0 || report.coughCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (report.snoreCount > 0) EventTypeChip("SNORE_LIKE", report.snoreCount)
                            if (report.speechCount > 0) EventTypeChip("SPEECH_LIKE", report.speechCount)
                            if (report.coughCount > 0) EventTypeChip("COUGH_LIKE", report.coughCount)
                            if (report.impactCount > 0) EventTypeChip("IMPACT_NOISE", report.impactCount)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { onNavigateToSessionDetail(report.sessionId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("查看完整报告")
                    }
                }
                Spacer(modifier = Modifier.height(SleepDimens.sectionGap))
            }

            if (state.lastReport == null && !state.isLoading && !state.isRecording) {
                SleepCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Icon(
                            Icons.Filled.NightsStay,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "还没有记录",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "开始第一次睡眠记录，探索你的夜间声音",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                Spacer(modifier = Modifier.height(SleepDimens.sectionGap))
            }

            if (state.recentEventCounts.isNotEmpty()) {
                SectionHeader(title = "近期趋势")
                Spacer(modifier = Modifier.height(SleepDimens.itemGap))
                SleepCard {
                    RecentEventChart(eventCounts = state.recentEventCounts)
                }
            }
        }
    }
}

@Composable
private fun RecordingOrb(
    currentRms: Float,
    eventCount: Int,
    onNavigateToRecording: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breath")

    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val rmsNormalized = ((currentRms).coerceIn(0f, 3000f)) / 3000f
    val pulseScale = 1f + rmsNormalized * 0.15f

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val baseRadius = size.minDimension / 2 * 0.55f

                for (i in 3 downTo 1) {
                    val ringRadius = baseRadius * breathScale * pulseScale * (1f + i * 0.18f)
                    val alpha = glowAlpha / (i * 1.5f)
                    drawCircle(
                        color = primaryColor.copy(alpha = alpha),
                        radius = ringRadius,
                        center = center
                    )
                }

                drawCircle(
                    color = primaryContainerColor,
                    radius = baseRadius * breathScale * pulseScale,
                    center = center
                )

                drawCircle(
                    color = primaryColor.copy(alpha = 0.3f),
                    radius = baseRadius * breathScale * pulseScale,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF60C080))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "正在守护",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$eventCount 段声音",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNavigateToRecording,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("查看详情", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun IdleOrb(
    onNavigateToRecording: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer

    val infiniteTransition = rememberInfiniteTransition(label = "idleBreath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleBreathScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val baseRadius = size.minDimension / 2 * 0.55f

                drawCircle(
                    color = primaryContainerColor.copy(alpha = 0.4f),
                    radius = baseRadius * 1.3f * breathScale,
                    center = center
                )

                drawCircle(
                    color = primaryContainerColor,
                    radius = baseRadius * breathScale,
                    center = center
                )

                drawCircle(
                    color = primaryColor.copy(alpha = 0.2f),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "准备入睡",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val btnPulse by infiniteTransition.animateFloat(
            initialValue = 0.97f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "btnPulse"
        )

        Button(
            onClick = onNavigateToRecording,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .graphicsLayer { scaleX = btnPulse; scaleY = btnPulse },
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("开始睡眠记录", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (granted) Color(0xFF60C080) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = "已授权", modifier = Modifier.size(20.dp), tint = Color(0xFF60C080))
        }
    }
}

@Composable
private fun StatItemMini(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecentEventChart(eventCounts: List<Int>) {
    if (eventCounts.isEmpty()) return

    val maxCount = eventCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
    val barColor = MaterialTheme.colorScheme.primary
    val barColorFaded = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        eventCounts.forEachIndexed { index, count ->
            val heightFraction = count.toFloat() / maxCount
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Canvas(modifier = Modifier.size(width = 24.dp, height = 50.dp)) {
                    val barHeight = size.height * heightFraction.coerceIn(0.05f, 1f)
                    drawRoundRect(
                        color = if (index == eventCounts.lastIndex) barColor else barColorFaded,
                        topLeft = Offset(0f, size.height - barHeight),
                        size = androidx.compose.ui.geometry.Size(size.width, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                }
            }
        }
    }
}
