# GitHub Actions 自动构建指南

## 快速开始

### 步骤1：推送到GitHub

如果您还没有GitHub仓库，需要先创建：

1. 访问 https://github.com/new
2. 创建新仓库（命名为：zhengshu）
3. 初始化本地Git仓库并推送：

```powershell
cd C:\Users\15070\Documents\trae_projects\zs
git init
git add .
git commit -m "Initial commit: 证枢Android应用"
git branch -M main
git remote add origin https://github.com/你的用户名/zhengshu.git
git push -u origin main
```

### 步骤2：触发构建

推送代码后，GitHub Actions会自动开始构建。您也可以手动触发：

1. 访问：https://github.com/你的用户名/zhengshu/actions
2. 点击"Build Android APK"工作流
3. 点击"Run workflow"按钮
4. 点击绿色的"Run workflow"确认

### 步骤3：下载APK

构建完成后（约5-10分钟）：

1. 访问：https://github.com/你的用户名/zhengshu/actions
2. 点击最新的构建任务
3. 滚动到底部"Artifacts"部分
4. 下载：
   - `app-debug` - 调试版APK
   - `app-release` - 发布版APK（未签名）

## 工作流说明

`.github/workflows/build.yml`文件定义了自动构建流程：

| 步骤 | 说明 |
|------|------|
| Checkout code | 检出代码 |
| Set up JDK 17 | 安装Java 17 |
| Setup Android SDK | 安装Android SDK |
| Grant execute permission | 赋予gradlew执行权限 |
| Build Debug APK | 构建调试版APK |
| Build Release APK | 构建发布版APK |
| Upload Debug APK | 上传调试版APK |
| Upload Release APK | 上传发布版APK |
| Build Summary | 生成构建摘要 |

## 本地测试

如果您想在本地测试构建，可以：

### 使用Docker（推荐）

```powershell
# 拉取Android构建镜像
docker pull openjdk:17-jdk-slim

# 运行构建容器
docker run --rm -v ${PWD}:/workspace -w /workspace/android openjdk:17-jdk-slim bash -c "./gradlew assembleDebug"
```

### 使用WSL2（Windows Subsystem for Linux）

```powershell
# 在WSL2中运行
wsl
cd /mnt/c/Users/15070/Documents/trae_projects/zs/android
./gradlew assembleDebug
```

## 签名APK（可选）

GitHub Actions构建的Release APK是未签名的，无法直接安装到设备。

### 方法1：使用apksigner

1. 下载apksigner：https://github.com/patrickfav/ultra-apk-signer/releases
2. 运行：
```powershell
java -jar apksigner.jar sign --ks your-keystore.jks --ks-key-alias key-alias --out signed.apk app-release-unsigned.apk
```

### 方法2：使用Android Studio

1. 在Android Studio中打开项目
2. Build → Generate Signed Bundle / APK
3. 选择APK
4. 创建或选择密钥库
5. 构建签名的APK

## 故障排除

### 构建失败

1. 查看Actions日志，找到错误信息
2. 常见问题：
   - 依赖下载失败：等待重试
   - 编译错误：检查代码语法
   - 资源缺失：检查res目录

### APK无法安装

1. 确保下载的是`app-debug`（调试版）
2. 或使用签名后的APK
3. 检查Android版本兼容性（需要Android 6.0+）

## 自动化脚本

### 一键推送并构建

创建`push-and-build.ps1`：

```powershell
# 添加所有文件
git add .

# 提交
git commit -m "Update: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"

# 推送
git push

# 等待构建
Write-Host "代码已推送，等待GitHub Actions构建..."
Write-Host "访问以下链接查看构建进度："
Write-Host "https://github.com/你的用户名/zhengshu/actions"
```

使用方法：
```powershell
.\push-and-build.ps1
```

## 下一步

构建成功后，您可以：

1. **安装APK到设备**
   - 将APK传输到Android设备
   - 在设备上打开APK文件
   - 允许安装未知来源应用
   - 安装并运行

2. **测试功能**
   - 测试AI风险识别
   - 测试存证功能
   - 测试UI交互

3. **继续开发**
   - 在Android Studio中打开项目
   - 修改代码
   - 提交并推送
   - 自动构建新版本

需要我帮您做什么吗？
