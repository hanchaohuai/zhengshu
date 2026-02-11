# Android开发环境安装指南

## 当前环境状态

| 组件 | 状态 |
|--------|--------|
| Java | ✅ 已安装 (Java 25.0.1) |
| Gradle | ❌ 未安装 |
| Android SDK | ❌ 未安装 |
| Android Studio | ❌ 未安装 |

## 安装选项

### 选项1：安装Android Studio（推荐）

Android Studio包含完整的Android SDK和开发工具。

#### 步骤1：下载Android Studio

1. 访问：https://developer.android.com/studio
2. 下载Windows版本（约1.2GB）
3. 运行安装程序

#### 步骤2：安装Android Studio

1. 运行下载的安装程序
2. 选择"Standard"安装类型
3. 选择安装路径（建议：`C:\Program Files\Android\Android Studio`）
4. 等待安装完成（约10-20分钟）

#### 步骤3：配置Android SDK

1. 打开Android Studio
2. 进入：Tools → SDK Manager
3. 安装以下SDK版本：
   - Android 14.0 (API 34)
   - Android 13.0 (API 33)
   - Android 12.0 (API 31)
   - Android 6.0 (API 23)
4. 安装SDK Build-Tools：34.0.0
5. 安装SDK Platform-Tools

#### 步骤4：配置环境变量

1. 右键"此电脑" → 属性 → 高级系统设置 → 环境变量
2. 新建系统变量：
   - 变量名：`ANDROID_HOME`
   - 变量值：`C:\Users\你的用户名\AppData\Local\Android\Sdk`
3. 编辑Path变量，添加：
   - `%ANDROID_HOME%\platform-tools`
   - `%ANDROID_HOME%\tools`
   - `%ANDROID_HOME%\tools\bin`

#### 步骤5：验证安装

打开PowerShell，运行：
```powershell
echo $env:ANDROID_HOME
adb version
```

### 选项2：仅安装Android SDK（轻量级）

如果您不需要完整的IDE，可以只安装SDK。

#### 使用命令行工具安装（Command Line Tools）

1. 下载Command Line Tools：
   https://dl.google.com/android/repository/commandlinetools-win-94773866_latest.zip

2. 解压到：`C:\Android\Sdk`

3. 打开PowerShell，运行：
```powershell
cd C:\Android\Sdk\cmdline-tools\latest\bin
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

4. 设置环境变量（同选项1）

### 选项3：使用GitHub Actions自动构建（无需本地安装）

如果不想在本地安装，可以使用GitHub Actions自动构建APK。

#### 创建GitHub Actions工作流

在项目根目录创建`.github/workflows/build.yml`：

```yaml
name: Build Android APK

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
      
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Setup Android SDK
      uses: android-actions/setup-android@v2
      
    - name: Grant execute permission for gradlew
      run: chmod +x android/gradlew
      
    - name: Build Debug APK
      run: |
        cd android
        ./gradlew assembleDebug
        
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: android/app/build/outputs/apk/debug/app-debug.apk
```

#### 使用方法

1. 将项目推送到GitHub
2. 在GitHub Actions页面点击"Build Android APK" → "Run workflow"
3. 等待构建完成（约5-10分钟）
4. 下载生成的APK

### 选项4：使用在线构建服务

#### 使用Gradle Play Service

1. 访问：https://scans.gradle.com/service
2. 上传项目或提供GitHub仓库链接
3. 选择构建配置
4. 下载生成的APK

## 快速安装脚本（PowerShell）

如果您选择选项2（仅SDK），可以运行以下脚本：

```powershell
# 安装Android SDK脚本
$ErrorActionPreference = "Stop"

# 设置SDK路径
$sdkPath = "C:\Android\Sdk"
$toolsPath = "$sdkPath\cmdline-tools\latest"

# 创建目录
New-Item -ItemType Directory -Force -Path $sdkPath
New-Item -ItemType Directory -Force -Path $toolsPath

# 下载Command Line Tools
Write-Host "下载Android Command Line Tools..."
$downloadUrl = "https://dl.google.com/android/repository/commandlinetools-win-94773866_latest.zip"
$zipPath = "$env:TEMP\commandlinetools.zip"
Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath

# 解压
Write-Host "解压文件..."
Expand-Archive -Path $zipPath -DestinationPath $toolsPath -Force

# 安装SDK包
Write-Host "安装Android SDK..."
& "$toolsPath\bin\sdkmanager.bat" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 设置环境变量
Write-Host "设置环境变量..."
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdkPath, "User")
$path = [Environment]::GetEnvironmentVariable("Path", "User")
[Environment]::SetEnvironmentVariable("Path", "$path;$sdkPath\platform-tools", "User")

Write-Host "安装完成！"
Write-Host "请重新打开PowerShell以使环境变量生效"
Write-Host "SDK路径: $sdkPath"
```

## 验证安装

安装完成后，运行以下命令验证：

```powershell
# 检查Java
java -version

# 检查Android SDK
echo $env:ANDROID_HOME

# 检查adb
adb version

# 检查sdkmanager
sdkmanager --version
```

## 构建APK

安装完成后，在项目目录运行：

```powershell
cd android
.\gradlew.bat assembleDebug
```

构建成功后，APK位于：
```
android\app\build\outputs\apk\debug\app-debug.apk
```

## 常见问题

### Q1: sdkmanager无法运行？
A: 确保已接受许可：
```powershell
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
```

### Q2: Gradle下载慢？
A: 配置国内镜像，在`android/build.gradle.kts`中添加：
```kotlin
repositories {
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    google()
    mavenCentral()
}
```

### Q3: 构建失败？
A: 清理并重新构建：
```powershell
cd android
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

## 推荐方案

对于快速体验，我建议：

1. **短期方案**：使用GitHub Actions自动构建（选项3）
2. **长期方案**：安装Android Studio（选项1），便于后续开发

您希望我帮您实施哪个方案？
