package com.gx.sleep.ui.screens.batteryguide

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gx.sleep.system.BatteryOptimizationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isIgnoring = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
    val isXiaomi = BatteryOptimizationHelper.isMiuiOrHyperOS()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("电池优化建议") },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isIgnoring)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isIgnoring) "已忽略电池优化" else "未忽略电池优化",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (!isIgnoring) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "建议关闭电池优化，确保睡眠记录一整晚不被系统中断",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            context.startActivity(BatteryOptimizationHelper.getBatteryOptimizationIntent())
                        }) {
                            Text("前往设置")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("通用建议", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    BulletText("保持手机充电，避免电量过低被系统杀死")
                    BulletText("关闭省电模式")
                    BulletText("不要强制停止 App")
                    BulletText("允许 App 后台运行")
                }
            }

            if (isXiaomi) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("小米/MIUI/HyperOS 特别设置", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "小米手机的省电策略较为严格，请按以下步骤设置：",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NumberedText("1. 打开「设置」>「电池与性能」")
                        NumberedText("2. 找到 gxSleep，选择「无限制」")
                        NumberedText("3. 打开「设置」>「应用管理」> gxSleep")
                        NumberedText("4. 开启「自启动」")
                        NumberedText("5. 在最近任务中长按 gxSleep，点击锁定图标")
                        NumberedText("6. 关闭「省电策略」或设为「无限制」")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "注意：不同 MIUI/HyperOS 版本菜单位置可能不同",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    context.startActivity(BatteryOptimizationHelper.getAppSettingsIntent())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("打开应用设置")
            }
        }
    }
}

@Composable
private fun BulletText(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun NumberedText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
