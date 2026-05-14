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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
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
import com.gx.sleep.data.datastore.ThemeMode
import com.gx.sleep.ui.components.SectionHeader
import com.gx.sleep.ui.components.SleepCard
import com.gx.sleep.ui.components.SleepDimens
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
            .padding(horizontal = SleepDimens.screenPaddingH)
            .padding(top = SleepDimens.screenPaddingTop, bottom = SleepDimens.screenPaddingBottom)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "调整录制参数和应用选项",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(SleepDimens.sectionGap))

        SectionHeader(title = "外观")
        Spacer(modifier = Modifier.height(SleepDimens.itemGap))

        SleepCard {
            Text("主题模式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "选择深色或浅色主题",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    val label = when (mode) {
                        ThemeMode.SYSTEM -> "跟随系统"
                        ThemeMode.LIGHT -> "浅色"
                        ThemeMode.DARK -> "深色"
                    }
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { scope.launch { settingsDataStore.updateThemeMode(mode) } },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                        icon = {}
                    ) {
                        Text(label)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(SleepDimens.sectionGap))

        // ── Recording settings ──
        SectionHeader(title = "记录设置")
        Spacer(modifier = Modifier.height(SleepDimens.itemGap))

        SleepCard {
            Text("录音保存方式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            AudioSaveModeSelector(
                currentMode = settings.audioSaveMode,
                onModeSelected = { mode ->
                    scope.launch { settingsDataStore.updateAudioSaveMode(mode) }
                }
            )
        }

        Spacer(modifier = Modifier.height(SleepDimens.itemGap))

        SleepCard {
            Text("检测灵敏度", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val level = when {
                    settings.sensitivity < 30 -> "低灵敏度"
                    settings.sensitivity < 70 -> "标准灵敏度"
                    else -> "高灵敏度"
                }
                val desc = when {
                    settings.sensitivity < 30 -> "只检测明显声音"
                    settings.sensitivity < 70 -> "平衡检测"
                    else -> "检测较轻声音"
                }
                Text(level, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(SleepDimens.sectionGap))

        // ── Background ──
        SectionHeader(title = "后台运行")
        Spacer(modifier = Modifier.height(SleepDimens.itemGap))

        SleepCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("实验 WakeLock", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(SleepDimens.itemGap))

        SettingNavItem("电池优化建议", "确保录制不被系统中断", onBatteryGuide)

        Spacer(modifier = Modifier.height(SleepDimens.sectionGap))

        // ── Privacy ──
        SectionHeader(title = "隐私与数据")
        Spacer(modifier = Modifier.height(SleepDimens.itemGap))

        SettingNavItem("隐私说明", "本地处理，不上传云端", onPrivacy)

        Spacer(modifier = Modifier.height(SleepDimens.sectionGap))

        // ── About ──
        SectionHeader(title = "关于")
        Spacer(modifier = Modifier.height(SleepDimens.itemGap))

        SettingNavItem("调试信息", "采样率、内存、录制状态", onDebug)
    }
}

@Composable
private fun SettingNavItem(label: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(SleepDimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SleepDimens.cardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}
