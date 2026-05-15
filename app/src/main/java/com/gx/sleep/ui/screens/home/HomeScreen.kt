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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.awaitCancellation
import com.gx.sleep.ui.components.EventTypeChip
import com.gx.sleep.ui.components.SleepCard
import com.gx.sleep.ui.components.SleepDimens
import com.gx.sleep.ui.components.StatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
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
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        try {
            awaitCancellation()
        } finally {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
            HomeHeader()

            Spacer(modifier = Modifier.height(SleepDimens.sectionGap))

            if (state.lastSessionCrashed) {
                CrashWarningCard(onDismiss = { viewModel.dismissCrashedWarning() })
                Spacer(modifier = Modifier.height(SleepDimens.itemGap))
            }

            val needsAudio = !state.hasAudioPermission
            val needsNotif = !state.hasNotificationPermission
            if (needsAudio || needsNotif) {
                PermissionCard(
                    needsAudio = needsAudio,
                    needsNotif = needsNotif,
                    hasAudio = state.hasAudioPermission,
                    hasNotif = state.hasNotificationPermission,
                    onGrantClick = {
                        val perms = mutableListOf<String>()
                        if (needsAudio) perms.add(Manifest.permission.RECORD_AUDIO)
                        if (needsNotif && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        if (perms.isNotEmpty()) permissionLauncher.launch(perms.toTypedArray())
                    },
                    onSettingsClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
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
                LastReportCard(
                    report = report,
                    onViewDetail = { onNavigateToSessionDetail(report.sessionId) }
                )
                Spacer(modifier = Modifier.height(SleepDimens.sectionGap))
            }

            if (state.lastReport == null && !state.isLoading && !state.isRecording) {
                EmptyHomeCard()
                Spacer(modifier = Modifier.height(SleepDimens.sectionGap))
            }

            if (state.recentEventCounts.isNotEmpty()) {
                TrendCard(eventCounts = state.recentEventCounts)
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    val dateFormat = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINESE) }
    val today = remember { dateFormat.format(Date()) }
    val hour = remember {
        val cal = java.util.Calendar.getInstance()
        cal.get(java.util.Calendar.HOUR_OF_DAY)
    }
    val greeting = when {
        hour < 6 -> "夜深了"
        hour < 12 -> "早上好"
        hour < 18 -> "下午好"
        else -> "晚上好"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = today,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CrashWarningCard(onDismiss: () -> Unit) {
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
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    }
}

@Composable
private fun PermissionCard(
    needsAudio: Boolean,
    needsNotif: Boolean,
    hasAudio: Boolean,
    hasNotif: Boolean,
    onGrantClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    SleepCard {
        Text(
            "需要授权",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "以下权限用于睡眠记录功能",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (needsAudio) {
            PermissionRow(Icons.Outlined.Mic, "麦克风权限", "采集环境声音", hasAudio)
        }
        if (needsNotif) {
            PermissionRow(Icons.Outlined.Notifications, "通知权限", "显示录制状态", hasNotif)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onGrantClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("授权权限")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onSettingsClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("前往系统设置")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LastReportCard(
    report: com.gx.sleep.domain.model.SessionReport,
    onViewDetail: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "昨晚睡眠评分",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${report.sleepScore}",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 56.sp
                    )
                }
                val quietDesc = when {
                    report.quietPercent > 90 -> "很安静"
                    report.quietPercent > 70 -> "较安静"
                    report.quietPercent > 50 -> "有声音"
                    else -> "较嘈杂"
                }
                val quietColor = if (report.quietPercent > 70) Color(0xFF60C080) else Color(0xFFE8A040)
                StatusBadge(text = quietDesc, color = quietColor)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ReportStatItem("时长", report.formattedDuration, Modifier.weight(1f))
                ReportStatItem("声音", "${report.totalEvents} 段", Modifier.weight(1f))
                ReportStatItem("安静", "${report.quietPercent.toInt()}%", Modifier.weight(1f))
            }

            if (report.snoreCount > 0 || report.speechCount > 0 || report.coughCount > 0 || report.impactCount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
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

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = onViewDetail,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("查看完整报告", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ReportStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
    }
}

@Composable
private fun EmptyHomeCard() {
    SleepCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                Icons.Filled.NightsStay,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "还没有记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "点击上方按钮，开始第一次睡眠记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TrendCard(eventCounts: List<Int>) {
    SleepCard {
        Text(
            "近期趋势",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "最近 ${eventCounts.size} 次记录的声音事件数",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        RecentEventChart(eventCounts = eventCounts)
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
                    radius = baseRadius * breathScale,
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
            Icon(
                Icons.Default.NightsStay,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
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
                        size = Size(size.width, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
        }
    }
}
