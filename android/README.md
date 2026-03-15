# 证枢 (ZhengShu) - Android应用

## 项目简介

证枢是一款移动端反诈存证维权一体化APP，实现"AI风险预警-全链路存证-法律文书生成-司法平台直连-硬件联动"全流程闭环。

## 技术栈

- **语言**: Kotlin 1.9.20
- **UI框架**: Jetpack Compose
- **最低SDK**: Android 6.0 (API 23)
- **目标SDK**: Android 14 (API 34)
- **架构**: MVVM + Clean Architecture
- **数据库**: Room
- **加密**: Android Keystore + AES-256
- **依赖注入**: 手动实现

## 项目结构

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/zhengshu/
│   │   │   ├── data/              # 数据层
│   │   │   │   ├── local/        # 本地数据库
│   │   │   │   ├── model/        # 数据模型
│   │   │   │   └── repository/   # 数据仓库
│   │   │   ├── services/          # 业务服务
│   │   │   │   ├── ai/           # AI风险识别服务
│   │   │   │   ├── evidence/      # 存证服务
│   │   │   │   ├── legal/        # 法律文书服务
│   │   │   │   ├── judiciary/     # 司法平台服务
│   │   │   │   └── hardware/     # 硬件通信服务
│   │   │   ├── ui/               # UI层
│   │   │   │   ├── screens/       # 页面
│   │   │   │   ├── components/    # 组件
│   │   │   │   ├── theme/         # 主题
│   │   │   │   └── viewmodel/    # ViewModel
│   │   │   ├── utils/            # 工具类
│   │   │   ├── ZhengShuApplication.kt
│   │   │   └── MainActivity.kt
│   │   ├── res/                  # 资源文件
│   │   ├── assets/               # 资产文件
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 核心功能

### 1. AI风险识别模块

#### 多源数据采集服务
- **DataCollectionService**: 无障碍服务，采集聊天文本和行为数据
- **SmsReceiver**: 短信接收器，采集短信内容
- **DataCollector**: 数据收集器，统一管理数据流

#### 关键词库
- **fraud_keywords.json**: 500+诈骗关键词，包含9大类别
  - 投资诈骗
  - 冒充身份诈骗
  - 网络贷款诈骗
  - 兼职刷单诈骗
  - 网络赌博诈骗
  - 杀猪盘诈骗
  - 钓鱼网站诈骗
  - 退款诈骗
  - 中奖诈骗

#### 风险识别引擎
- **RiskDetectionEngine**: 三重风险识别引擎
  - 关键词匹配
  - 行为逻辑校验
  - 平台和发送者分析
- **BehaviorRuleEngine**: 行为规则引擎

#### 预警弹窗UI
- **RiskAlertDialog**: 风险预警弹窗
  - 显示风险等级（高/中/低）
  - 显示风险原因
  - 显示检测到的关键词
  - 三个操作选项：忽略、标记误报、启动存证

### 2. 存证模块

#### 录屏服务
- **ScreenRecordService**: 屏幕录制服务
  - 支持MediaProjection API
  - 前台服务通知
  - 录屏状态管理

#### 聊天记录提取
- **ChatMessageExtractor**: 聊天记录提取器
  - 短信提取
  - 微信/QQ/钉钉提取（预留接口）
  - 撤回/删除消息提取（预留接口）

#### 加密与封装
- **EncryptionManager**: 加密管理器
  - AES-256加密
  - Android Keystore密钥管理
  - EncryptedSharedPreferences
- **HashUtils**: 哈希工具
  - SHA-256哈希计算
  - MD5哈希计算
  - .proof格式封装

#### 本地存储管理
- **EvidenceStorageManager**: 存证存储管理器
  - 证据保存/删除
  - 存储空间管理
  - 自动清理（180天未使用）
  - 清理提醒（提前7天）

### 3. 主界面和导航

- **MainScreen**: 主界面
  - 底部导航栏（6个标签页）
  - 首页：风险识别服务状态、最近风险检测、快速操作
  - 存证页：证据包列表
  - 文书页：法律文书列表
  - 司法页：司法平台对接
  - 硬件页：硬件设备状态
  - 设置页：应用设置

## 构建APK

### 前置要求

1. 安装Android Studio
2. 安装JDK 17
3. 配置Android SDK（API 23-34）
4. 配置Gradle 8.2+

### 构建步骤

#### 方法1: 使用Android Studio
1. 打开Android Studio
2. 选择"Open an Existing Project"
3. 选择`android`目录
4. 等待Gradle同步完成
5. 点击"Build" -> "Build Bundle(s) / APK(s)" -> "Build APK(s)"
6. 构建完成后，APK位于`android/app/build/outputs/apk/debug/app-debug.apk`

#### 方法2: 使用命令行
```bash
cd android
./gradlew assembleDebug
```

构建完成后，APK位于`android/app/build/outputs/apk/debug/app-debug.apk`

### 签名APK

#### 生成签名密钥
```bash
keytool -genkey -v -keystore zhengshu.keystore -alias zhengshu -keyalg RSA -keysize 2048 -validity 10000
```

#### 配置签名
在`android/app/build.gradle.kts`中配置签名信息。

#### 构建签名APK
```bash
./gradlew assembleRelease
```

## 运行应用

### 在模拟器中运行
1. 在Android Studio中创建或选择一个模拟器
2. 点击"Run"按钮或按Shift+F10

### 在真机上运行
1. 启用开发者选项和USB调试
2. 连接设备到电脑
3. 在Android Studio中选择设备
4. 点击"Run"按钮

## 权限说明

应用需要以下权限：
- `INTERNET`: 网络访问
- `ACCESS_NETWORK_STATE`: 网络状态
- `RECORD_AUDIO`: 录音（用于证据采集）
- `CAMERA`: 相机（用于证据采集）
- `WRITE_EXTERNAL_STORAGE`: 写入外部存储
- `READ_EXTERNAL_STORAGE`: 读取外部存储
- `FOREGROUND_SERVICE`: 前台服务
- `SYSTEM_ALERT_WINDOW`: 悬浮窗
- `POST_NOTIFICATIONS`: 通知
- `BLUETOOTH`: 蓝牙
- `BLUETOOTH_ADMIN`: 蓝牙管理
- `BLUETOOTH_SCAN`: 蓝牙扫描
- `BLUETOOTH_CONNECT`: 蓝牙连接
- `NFC`: NFC
- `USE_BIOMETRIC`: 生物识别
- `USE_FINGERPRINT`: 指纹识别

## 合规说明

### 隐私合规
- 符合《个人信息保护法》
- 数据采集需用户逐项授权
- 默认关闭敏感数据采集
- 数据仅在设备端处理

### 司法合规
- 符合《电子证据规定》
- 证据生成、存储、传输符合规范
- 提供合规说明文档

### 权限合规
- 仅申请必要权限
- 无权限不影响核心功能使用

## 性能指标

| 指标 | 目标值 |
|------|--------|
| AI风险识别响应时间 | <300ms |
| CPU占用率 | <15% |
| 内存占用 | <200MB |
| 存证启动时间 | <300ms |
| 录屏帧率 | ≥30fps |
| 蓝牙传输速率 | ≥1MB/s |
| APP启动时间 | <2s |
| 页面加载时间 | <1s |

## 开发规范

### 代码风格
- 统一命名规范
- 函数/类/变量命名清晰
- 每个复杂逻辑块添加中文注释

### 错误处理
- 所有API调用捕获异常
- 硬件通信捕获异常
- 数据采集环节捕获异常
- 返回详细错误信息

### 安全防护
- 防止SQL注入
- 防止数据泄露
- 防止恶意篡改证据

## 后续扩展

1. **场景扩展**: 职场纠纷存证、合同纠纷存证等
2. **功能扩展**: AI法律咨询、在线律师对接
3. **平台扩展**: Web端、小程序端
4. **硬件扩展**: 更多硬件设备支持
5. **地域扩展**: 海外市场适配

## 联系方式

如有问题，请联系开发团队。
