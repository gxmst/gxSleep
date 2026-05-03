package com.gx.sleep.ui.screens.sessionlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gx.sleep.data.local.entity.SessionStatus
import com.gx.sleep.data.local.entity.SleepSessionEntity
import com.gx.sleep.ui.components.ConfirmDialog
import com.gx.sleep.ui.components.EmptyState
import com.gx.sleep.ui.components.SleepDimens
import com.gx.sleep.ui.components.StatusBadge
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionListScreen(
    onSessionClick: (Long) -> Unit,
    viewModel: SessionListViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    val dateFormat = SimpleDateFormat("M月d日 HH:mm", Locale.CHINESE)

    if (showDeleteDialog != null) {
        ConfirmDialog(
            title = "删除记录",
            message = "确定要删除这条记录吗？删除后无法恢复。",
            confirmText = "删除",
            isDanger = true,
            onConfirm = {
                scope.launch { showDeleteDialog?.let { viewModel.deleteSession(it) } }
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SleepDimens.screenPaddingH)
            .padding(top = SleepDimens.screenPaddingTop, bottom = SleepDimens.screenPaddingBottom)
    ) {
        Text(
            text = "历史记录",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "查看每晚的睡眠声音记录",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(SleepDimens.sectionGap))

        if (sessions.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                },
                title = "还没有记录",
                subtitle = "开始第一次睡眠记录吧"
            )
        } else {
            // P2: Filter out RUNNING sessions - they have no valid report data
            val completedSessions = sessions.filter { it.status != SessionStatus.RUNNING }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(SleepDimens.itemGap)
            ) {
                items(completedSessions, key = { it.id }) { session ->
                    SessionListItem(
                        session = session,
                        dateFormat = dateFormat,
                        onClick = { onSessionClick(session.id) },
                        onDelete = { showDeleteDialog = session.id }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SessionListItem(
    session: SleepSessionEntity,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val hours = session.duration / 3600000
    val minutes = (session.duration % 3600000) / 60000
    val durationText = if (hours > 0) "${hours}小时${minutes}分钟" else "${minutes}分钟"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(SleepDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SleepDimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(Date(session.startTime)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (statusText, statusColor) = when (session.status) {
                        SessionStatus.COMPLETED -> "记录完成" to Color(0xFF60C080)
                        SessionStatus.CRASHED -> "意外中断" to MaterialTheme.colorScheme.error
                        SessionStatus.RUNNING -> "记录中" to MaterialTheme.colorScheme.primary
                        SessionStatus.STOPPED_BY_SYSTEM -> "系统停止" to Color(0xFFE8A040)
                    }
                    StatusBadge(text = statusText, color = statusColor)
                    if (session.duration > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
