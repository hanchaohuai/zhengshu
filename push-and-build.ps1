# 一键推送并构建APK

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  证枢 - 自动构建脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查是否在Git仓库中
if (-not (Test-Path ".git")) {
    Write-Host "❌ 错误：当前目录不是Git仓库" -ForegroundColor Red
    Write-Host ""
    Write-Host "请先运行以下命令初始化Git仓库：" -ForegroundColor Yellow
    Write-Host "  git init" -ForegroundColor White
    Write-Host ""
    Write-Host "然后配置远程仓库：" -ForegroundColor Yellow
    Write-Host "  git remote add origin https://github.com/你的用户名/zhengshu.git" -ForegroundColor White
    Write-Host ""
    exit 1
}

# 检查是否有未提交的更改
$gitStatus = git status --porcelain
if ($gitStatus) {
    Write-Host "📝 检测到未提交的更改..." -ForegroundColor Yellow
    
    # 添加所有文件
    git add .
    
    # 提交
    $commitMessage = "Update: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    git commit -m $commitMessage
    
    Write-Host "✅ 已提交更改" -ForegroundColor Green
} else {
    Write-Host "✅ 没有未提交的更改" -ForegroundColor Green
}

# 检查远程仓库
$remoteUrl = git remote get-url origin 2>$null
if (-not $remoteUrl) {
    Write-Host ""
    Write-Host "⚠️  警告：未配置远程仓库" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "请先配置远程仓库：" -ForegroundColor Yellow
    Write-Host "  git remote add origin https://github.com/你的用户名/zhengshu.git" -ForegroundColor White
    Write-Host ""
    
    $configure = Read-Host "是否现在配置远程仓库？(y/n)"
    if ($configure -eq "y" -or $configure -eq "Y") {
        $username = Read-Host "请输入GitHub用户名"
        $remoteUrl = "https://github.com/$username/zhengshu.git"
        git remote add origin $remoteUrl
        Write-Host "✅ 已配置远程仓库：$remoteUrl" -ForegroundColor Green
    } else {
        Write-Host "❌ 已取消" -ForegroundColor Red
        exit 1
    }
}

# 推送到GitHub
Write-Host ""
Write-Host "🚀 正在推送到GitHub..." -ForegroundColor Cyan
git push -u origin main

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ 推送成功！" -ForegroundColor Green
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  下一步操作" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. 访问GitHub Actions页面：" -ForegroundColor White
    Write-Host "   https://github.com/$($remoteUrl -replace 'https://github.com/', '')/actions" -ForegroundColor Blue
    Write-Host ""
    Write-Host "2. 点击 'Build Android APK' 工作流" -ForegroundColor White
    Write-Host ""
    Write-Host "3. 点击 'Run workflow' 按钮触发构建" -ForegroundColor White
    Write-Host ""
    Write-Host "4. 等待构建完成（约5-10分钟）" -ForegroundColor White
    Write-Host ""
    Write-Host "5. 在构建完成后，下载APK：" -ForegroundColor White
    Write-Host "   - app-debug (调试版)" -ForegroundColor Yellow
    Write-Host "   - app-release (发布版)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    
    # 询问是否打开浏览器
    $openBrowser = Read-Host "是否打开GitHub Actions页面？(y/n)"
    if ($openBrowser -eq "y" -or $openBrowser -eq "Y") {
        $actionsUrl = "https://github.com/$($remoteUrl -replace 'https://github.com/', '')/actions"
        Start-Process $actionsUrl
    }
} else {
    Write-Host "❌ 推送失败" -ForegroundColor Red
    Write-Host ""
    Write-Host "请检查：" -ForegroundColor Yellow
    Write-Host "1. 网络连接" -ForegroundColor White
    Write-Host "2. GitHub凭据" -ForegroundColor White
    Write-Host "3. 远程仓库URL" -ForegroundColor White
    exit 1
}
