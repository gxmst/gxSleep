package com.gx.sleep.ui.screens.recording

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gx.sleep.service.SleepRecordingService
import com.gx.sleep.ui.components.AudioLevelIndicator
import com.gx.sleep.ui.components.ConfirmDialog
import com.gx.sleep.ui.components.SleepCard
import com.gx.sleep.ui.components.SleepDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    onBack: () -> Unit,
    onStopAndShowReport: (Long) -> Unit
) {
    val context = LocalContext.current
    val isRecording by SleepRecordingService.isRecording.collectAsState()
    val currentRms by SleepRecordingService.currentRms.collectAsState()
    val eventCount by SleepRecordingService.eventCount.collectAsState()
    val sessionId by SleepRecordingService.sessionId.collectAsState()

    var showStopDialog by remember { mutableStateOf(false) }
    // P1: Track whether we ever saw recording=true on this page.
    // Only navigate to report after a true->false transition, not on initial false.
    var hasSeenRecording by remember { mutableStateOf(false) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            hasSeenRecording = true
        }
    }

    // P1: Listen for recordingCompleted event (emitted after DB flush + completeSession).
    // This is the safe time to navigate to the report.
    LaunchedEffect(Unit) {
        SleepRecordingService.recordingCompleted.collect { completedSessionId ->
            if (completedSessionId > 0) {
                onStopAndShowReport(completedSessionId)
            }
        }
    }

    if (showStopDialog) {
        ConfirmDialog(
            title = "停止记录",
            message = "确定要停止今晚的睡眠记录吗？停止后将生成睡眠报告。",
            confirmText = "停止记录",
            isDanger = true,
            onConfirm = {
                showStopDialog = false
                SleepRecordingService.stopService(context)
                // Don't navigate yet — wait for recordingCompleted event
            },
            onDismiss = { showStopDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("睡眠记录") },
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
                .padding(horizontal = SleepDimens.screenPaddingH)
                .padding(top = 24.dp, bottom = SleepDimens.screenPaddingBottom),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Status ──
                if (isRecording) {
                    // Pulsing indicator
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF60C080).copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "正在记录中",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "手机放在枕边，安心入睡",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "已停止",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // ── Audio level ──
                AudioLevelIndicator(rms = currentRms, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(24.dp))

                // ── Stats grid ──
                SleepCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$eventCount",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "段声音",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(SleepDimens.sectionGap))

                // ── Privacy note ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(14.dp)
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "本地分析，不上传录音",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Bottom ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "锁屏后会继续记录，放心休息",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showStopDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = isRecording
                ) {
                    Text("停止记录", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
