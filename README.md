# gxSleep - 睡眠声音监测 App

一个纯原生 Android 睡眠声音监测应用，仅依赖手机麦克风，无需手环或其他设备。

## 重要声明

**本 App 不是医疗设备，不提供任何医学诊断。** 不能检测睡眠呼吸暂停、睡眠呼吸障碍或其他任何疾病。App 的定位是"睡眠声音日志"和"夜间环境声音分析工具"。如有睡眠问题请咨询专业医生。

## 功能特性

- **睡眠声音记录**：使用手机麦克风采集夜间环境声音
- **实时音量监测**：显示当前环境声音的音量级别
- **声音事件检测**：自动检测和分类鼾声、梦话、咳嗽、突发噪音等
- **睡眠声音评分**：基于夜间声音环境生成 0-100 分的评分（非医学评分）
- **音量曲线图**：可视化展示整晚的声音变化
- **隐私优先**：当前版本不保存任何录音文件，仅保存统计数据
- **低功耗运行**：使用前台服务确保锁屏后持续记录

## 当前版本数据保存说明

**v0.1 版本仅保存以下数据：**
- 记录会话的开始/结束时间和时长
- 每秒的声音特征统计值（RMS、dBFS、零交叉率等）
- 检测到的声音事件的时间、类型和置信度
- 设备信息和电量记录

**当前版本不保存任何音频文件。** 事件片段保存功能将在后续版本中提供。

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- AudioRecord PCM 16-bit 采集
- Room 数据库
- DataStore 偏好设置
- Kotlin Coroutines / Flow
- MVVM + Repository 架构
- Foreground Service

## 权限说明

| 权限 | 用途 |
|------|------|
| RECORD_AUDIO | 采集环境声音用于分析 |
| POST_NOTIFICATIONS | (Android 13+) 显示录制状态通知 |
| FOREGROUND_SERVICE | 运行前台录制服务 |
| FOREGROUND_SERVICE_MICROPHONE | (Android 14+) 前台麦克风服务 |
| WAKE_LOCK | 锁屏后保持 CPU 运行 |

**本 App 不需要网络权限，不包含任何广告或统计 SDK。**

## 如何用 GitHub Actions 云端打包 APK 并安装到手机

### 步骤一：Fork 或使用本仓库

1. 在 GitHub 上 Fork 本仓库，或直接使用你的仓库
2. 确保代码已推送到 `main` 分支

### 步骤二：触发 GitHub Actions 构建

1. 打开仓库页面，点击 **Actions** 标签
2. 在左侧选择 **Build Debug APK**
3. 点击 **Run workflow** 按钮
4. 选择 `main` 分支，点击绿色的 **Run workflow**
5. 等待构建完成（通常 3-5 分钟）

### 步骤三：下载 APK

1. 构建完成后，点击构建任务进入详情页
2. 在 **Artifacts** 区域找到 `gxSleep-debug-apk`
3. 点击下载，得到一个 zip 文件
4. 解压 zip 得到 `app-debug.apk`

### 步骤四：安装到手机

1. 将 `app-debug.apk` 传到手机（微信、QQ、网盘、USB 等）
2. 在手机上打开文件管理器，找到 apk 文件
3. 点击安装（需要开启"允许安装未知来源应用"）
4. 首次启动时授予麦克风和通知权限

### 小米手机安装步骤

小米手机安装第三方 APK 需要额外步骤：

1. 将 `app-debug.apk` 传到手机
2. 打开「文件管理」找到 apk 文件
3. 点击安装，如果提示"禁止安装未知来源应用"
4. 点击「设置」> 开启「允许来自此来源的应用」
5. 返回安装界面完成安装
6. 首次启动时授予所有权限

### GitHub Actions 构建失败排查

如果 Actions 显示 **In progress** 超过 10 分钟或失败：

1. **查看日志**
   - 进入仓库 > Actions 标签 > 点击失败的构建
   - 点击 `build` job 展开步骤
   - 红色 ❌ 标记的步骤就是失败点
   - 点击该步骤查看完整错误日志

2. **常见失败原因**
   - `gradlew: Permission denied` → workflow 缺少 `chmod +x ./gradlew`
   - `Could not find or load main class` → gradle-wrapper.jar 缺失
   - `SDK not found` → 需要检查 compileSdk 版本
   - `OutOfMemoryError` → Gradle 内存不足，一般不会发生

3. **重新运行**
   - 在失败的构建页面点击 **Re-run all jobs**
   - 或者重新触发 `workflow_dispatch`

4. **本地验证**
   ```bash
   ./gradlew :app:assembleDebug --stacktrace
   ```
   如果本地成功但 Actions 失败，通常是环境差异问题

### 本地构建（可选）

如果需要本地构建，需要 JDK 17+ 和 Android SDK 35：

```bash
# 克隆仓库
git clone https://github.com/gxmst/gxSleep.git
cd gxSleep

# 构建 Debug APK
./gradlew :app:assembleDebug --stacktrace

# APK 位置
# app/build/outputs/apk/debug/app-debug.apk
```

## 真机测试验收流程

### 基础测试（5 分钟）

1. **安装 APK**
   - 通过 GitHub Actions 构建或本地构建获得 APK
   - 安装到 Android 手机

2. **权限授予**
   - 启动 App
   - 授予麦克风权限
   - (Android 13+) 授予通知权限
   - 确认首页不再显示权限警告

3. **开始录制**
   - 点击"开始睡眠记录"
   - 确认出现"正在初始化录音..."通知
   - 确认通知变为"睡眠声音记录进行中"
   - 确认首页显示"正在记录中"
   - 确认音量指示器有反应

4. **锁屏测试**
   - 按电源键锁屏
   - 等待 5 分钟
   - 解锁手机
   - 确认 App 仍在录制状态
   - 确认事件计数有增长

5. **停止录制**
   - 点击"停止记录"
   - 确认弹出二次确认对话框
   - 点击"停止"
   - 确认通知消失
   - 确认回到首页，显示睡眠报告

6. **验证报告**
   - 进入历史记录，确认有一条记录
   - 点击记录查看详情
   - 确认音量曲线图显示
   - 确认事件统计正确

### 通知停止测试

1. 开始录制
2. 锁屏
3. 下拉通知栏
4. 点击通知中的"停止记录"按钮
5. 确认录制停止，通知消失

### 小米/MIUI/HyperOS 测试（30 分钟）

1. **设置电池优化**
   - 进入 App 设置 > 电池优化建议
   - 按指引关闭电池优化
   - 设置允许自启动
   - 在最近任务中锁定 App

2. **长时间测试**
   - 开始录制
   - 锁屏
   - 等待 30 分钟
   - 解锁确认仍在录制
   - 停止并查看报告

### 整晚测试

1. 手机充满电或接充电器
2. 按上述设置完成电池优化
3. 睡前开始录制
4. 手机放在枕边
5. 次日早上停止录制
6. 查看完整睡眠报告
7. 确认时长约等于睡眠时长
8. 确认事件数量合理

## 小米/MIUI/HyperOS 优化建议

小米手机的省电策略较为严格，可能导致录制中断。请按以下步骤设置：

1. **关闭电池优化**
   - 设置 > 电池与性能 > 找到 gxSleep > 选择"无限制"

2. **允许自启动**
   - 设置 > 应用管理 > gxSleep > 开启"自启动"

3. **锁定后台任务**
   - 打开最近任务列表
   - 长按 gxSleep 卡片
   - 点击锁定图标

4. **关闭省电限制**
   - 设置 > 电池与性能 > 省电策略 > 选择"无限制"

**注意**：不同 MIUI/HyperOS 版本的菜单位置可能不同。

**WakeLock 说明**：App 默认不持有 WakeLock，前台服务是主要保活机制。如果锁屏录制中断（可在调试页面确认），可在设置中开启「实验 WakeLock」。开启后会增加少量耗电。

## 已知限制

- 声音分类使用规则算法，准确率有限
- 不能区分睡眠阶段
- 环境噪音较大时可能误报
- 部分设备可能被系统杀后台
- 不支持 Android 8.0 以下设备
- 长时间录制会消耗一定电量
- 当前版本不保存音频片段

## 后续优化路线

### v0.1 - 稳定采集和报告（当前版本）
- [x] AudioRecord PCM 采集
- [x] 前台服务保活
- [x] 基于规则的声音事件检测
- [x] 睡眠声音评分
- [x] 音量曲线图
- [x] GitHub Actions CI

### v0.2 - 事件片段保存
- [ ] 保存疑似事件前后音频片段
- [ ] 片段回放功能
- [ ] 存储空间管理

### v0.3 - 更好的鼾声规则分类
- [ ] 改进鼾声检测算法
- [ ] 周期性声音检测
- [ ] 降低误报率

### v0.4 - 本地 ML 模型分类
- [ ] 集成 TFLite 或 ONNX 模型
- [ ] 更准确的声音分类
- [ ] 请参考"如何接入本地 ML 模型"章节

### v0.5 - Health Connect 可选接入
- [ ] 写入睡眠数据到 Health Connect
- [ ] 读取其他睡眠数据

### v0.6 - 多设备适配和耗电基准测试
- [ ] 更多设备兼容性测试
- [ ] 优化功耗
- [ ] 适配 Android 15+

## 如何接入本地 ML 模型

### 使用 TensorFlow Lite

1. **添加依赖**
   ```kotlin
   // app/build.gradle.kts
   implementation("org.tensorflow:tensorflow-lite:2.14.0")
   ```

2. **实现分类器**
   ```kotlin
   class TFLiteSoundEventClassifier(
       context: Context,
       modelPath: String
   ) : SoundEventClassifier {
       private val interpreter: Interpreter
       
       init {
           val model = FileUtil.loadMappedFile(context, modelPath)
           interpreter = Interpreter(model)
       }
       
       override fun classify(...): Pair<SoundEventType, Float> {
           // Prepare input tensor
           // Run inference
           // Return classification result
       }
   }
   ```

3. **替换分类器**
   ```kotlin
   val classifier = TFLiteSoundEventClassifier(context, "sound_model.tflite")
   val detector = SoundEventDetector(classifier)
   ```

## 如何导出数据用于分析

### 方式一：ADB 导出数据库

```bash
# 导出 Room 数据库
adb pull /data/data/com.gx.sleep/databases/gx_sleep.db ./export/

# 使用 DB Browser for SQLite 查看
```

### 数据库表结构

- `sleep_sessions`：记录会话信息
- `sound_samples`：每秒声音特征数据
- `sound_events`：检测到的声音事件

## 项目结构

```
app/src/main/java/com/gx/sleep/
├── audio/              # 音频采集引擎
├── service/            # 前台录制服务
├── analysis/           # 声音分析和分类
├── data/               # 数据层（Room、DataStore、Repository）
├── domain/             # 领域模型
├── ui/                 # UI 层（Compose 界面）
├── system/             # 系统功能（权限、电池优化）
└── debug/              # 调试工具
```

## License

本项目仅供学习和个人使用。
