package com.gx.sleep.ui.screens.sessiondetail

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gx.sleep.data.local.entity.SoundEventEntity
import com.gx.sleep.domain.model.SoundEvent
import com.gx.sleep.ui.components.EventColors
import com.gx.sleep.ui.components.EventTypeChip
import com.gx.sleep.ui.components.SectionHeader
import com.gx.sleep.ui.components.SleepCard
import com.gx.sleep.ui.components.SleepDimens
import com.gx.sleep.ui.components.VolumeChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    onEventClick: (Long) -> Unit,
    viewModel: SessionDetailViewModel = viewModel()
) {
    val report by viewModel.report.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val eventsWithClips by viewModel.eventsWithClips.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    LaunchedEffect(sessionId) { viewModel.loadSession(sessionId) }

    val dateFormat = SimpleDateFormat("M月d日 HH:mm", Locale.CHINESE)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("睡眠报告") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopPlayback()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("正在生成报告...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (report == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("无法加载报告", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            val r = report!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SleepDimens.screenPaddingH)
                    .padding(top = 8.dp, bottom = SleepDimens.screenPaddingBottom)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "睡眠声音评分",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${r.sleepScore}",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val desc = when {
                            r.sleepScore >= 85 -> "昨晚睡眠环境很好"
                            r.sleepScore >= 70 -> "睡眠环境整体不错"
                            r.sleepScore >= 50 -> "有一些声音干扰"
                            else -> "环境比较嘈杂"
                        }
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "仅供自我观察，不构成医学诊断",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(SleepDimens.sectionGap))

                SectionHeader(title = "核心数据")
                Spacer(modifier = Modifier.height(SleepDimens.itemGap))
                SleepCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatCol("睡眠时长", r.formattedDuration, Modifier.weight(1f))
                        StatCol("鼾声", "${r.snoreCount} 次", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatCol("梦话", "${r.speechCount} 次", Modifier.weight(1f))
                        StatCol("安静比例", "${r.quietPercent.toInt()}%", Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(SleepDimens.sectionGap))

                SectionHeader(title = "记录时间")
                Spacer(modifier = Modifier.height(SleepDimens.itemGap))
                SleepCard {
                    TimeRow("入睡时间", dateFormat.format(Date(r.startTime)))
                    Spacer(modifier = Modifier.height(10.dp))
                    TimeRow("醒来时间", dateFormat.format(Date(r.endTime)))
                    Spacer(modifier = Modifier.height(10.dp))
                    TimeRow("记录时长", r.formattedDuration)
                }

                Spacer(modifier = Modifier.height(SleepDimens.sectionGap))

                SectionHeader(title = "声音环境")
                Spacer(modifier = Modifier.height(SleepDimens.itemGap))
                SleepCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatCol("安静比例", "${r.quietPercent.toInt()}%", Modifier.weight(1f))
                        StatCol("明显声音", "${r.totalEvents} 段", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatCol("平均音量", "${r.avgDbfs.toInt()} dB", Modifier.weight(1f))
                        StatCol("最高音量", "${r.maxDbfs.toInt()} dB", Modifier.weight(1f))
                    }

                    if (r.snoreCount > 0 || r.speechCount > 0 || r.coughCount > 0 || r.impactCount > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("声音类型", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (r.snoreCount > 0) EventTypeChip("SNORE_LIKE", r.snoreCount)
                            if (r.speechCount > 0) EventTypeChip("SPEECH_LIKE", r.speechCount)
                            if (r.coughCount > 0) EventTypeChip("COUGH_LIKE", r.coughCount)
                            if (r.impactCount > 0) EventTypeChip("IMPACT_NOISE", r.impactCount)
                            if (r.environmentNoiseCount > 0) EventTypeChip("ENVIRONMENT_NOISE", r.environmentNoiseCount)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(SleepDimens.sectionGap))

                if (r.volumeCurve.isNotEmpty()) {
                    SectionHeader(title = "声音变化曲线", subtitle = "颜色区域代表不同声音类型")
                    Spacer(modifier = Modifier.height(SleepDimens.itemGap))
                    SleepCard {
                        VolumeChart(
                            volumeCurve = r.volumeCurve,
                            events = r.events,
                            startTime = r.startTime,
                            endTime = r.endTime
                        )
                    }
                }

                Spacer(modifier = Modifier.height(SleepDimens.sectionGap))

                if (eventsWithClips.isNotEmpty()) {
                    SectionHeader(title = "录音回放", subtitle = "点击播放夜间声音片段")
                    Spacer(modifier = Modifier.height(SleepDimens.itemGap))
                    SleepCard {
                        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.CHINESE)
                        eventsWithClips.forEachIndexed { index, event ->
                            AudioClipItem(
                                event = event,
                                timeFmt = timeFmt,
                                isPlaying = playbackState.playingEventId == event.id && playbackState.isPlaying,
                                onPlayClick = {
                                    viewModel.togglePlayback(event.id, event.audioClipPath!!)
                                }
                            )
                            if (index < eventsWithClips.lastIndex) {
                                Spacer(modifier = Modifier.height(2.dp))
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(start = 28.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(SleepDimens.sectionGap))
                }

                if (r.events.isNotEmpty()) {
                    SectionHeader(title = "声音事件时间轴", subtitle = "点击查看详情")
                    Spacer(modifier = Modifier.height(SleepDimens.itemGap))
                    SleepCard {
                        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.CHINESE)
                        r.events.forEachIndexed { index, event ->
                            EventTimelineItem(
                                event = event,
                                timeFmt = timeFmt,
                                onClick = { onEventClick(event.id) }
                            )
                            if (index < r.events.lastIndex) {
                                Spacer(modifier = Modifier.height(2.dp))
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(start = 28.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(SleepDimens.sectionGap))
                }

                Text(
                    text = "仅供自我观察，不构成医学诊断",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TimeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatCol(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AudioClipItem(
    event: SoundEventEntity,
    timeFmt: SimpleDateFormat,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    val color = EventColors.forType(event.type)
    val label = EventColors.labelFor(event.type)
    val durationText = when {
        event.durationMs < 1000 -> "<1秒"
        event.durationMs < 10000 -> "%.1f秒".format(event.durationMs / 1000f)
        else -> "${event.durationMs / 1000}秒"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${timeFmt.format(Date(event.startTime))} · $durationText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = onPlayClick,
            modifier = Modifier.size(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EventTimelineItem(
    event: SoundEvent,
    timeFmt: SimpleDateFormat,
    onClick: () -> Unit
) {
    val color = EventColors.forType(event.type.name)
    val label = EventColors.labelFor(event.type.name)
    val durationText = when {
        event.durationMs < 1000 -> "<1秒"
        event.durationMs < 10000 -> "%.1f秒".format(event.durationMs / 1000f)
        else -> "${event.durationMs / 1000}秒"
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${timeFmt.format(Date(event.startTime))} · $durationText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${(event.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "›",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}
