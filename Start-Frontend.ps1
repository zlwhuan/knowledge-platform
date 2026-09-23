# Start Frontend with Dependency Check
# 杀死现有进程、检查依赖、启动前端

$ErrorActionPreference = "Stop"
Set-Location "D:\Projects\knowledge-platform\frontend"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Knowledge Platform Frontend Startup" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# 1. 检查Node.js环境
Write-Host "[1/3] Checking Node.js environment..." -ForegroundColor Yellow
try {
    $nodeVersion = & node --version 2>&1
    $npmVersion = & npm --version 2>&1
    Write-Host "  Node.js: $nodeVersion" -ForegroundColor Green
    Write-Host "  npm: $npmVersion" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Node.js not found. Please install Node.js 18 or later." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# 2. 杀死现有的Node进程（Vite开发服务器）
Write-Host ""
Write-Host "[2/3] Stopping existing frontend processes..." -ForegroundColor Yellow
$nodeProcesses = Get-Process node -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -like "*vite*" -or 
    $_.CommandLine -like "*dev*" -or
    $_.CommandLine -like "*frontend*"
}

if ($nodeProcesses) {
    $nodeProcesses | Stop-Process -Force -ErrorAction SilentlyContinue
    Write-Host "  Stopped $($nodeProcesses.Count) Node process(es)" -ForegroundColor Green
    Start-Sleep -Seconds 2
} else {
    Write-Host "  No existing frontend processes found" -ForegroundColor Gray
}

# 3. 安装依赖（如果需要）
Write-Host ""
Write-Host "[3/3] Starting Frontend on port 5173..." -ForegroundColor Yellow

if (-not (Test-Path "node_modules")) {
    Write-Host "  Installing dependencies..." -ForegroundColor Yellow
    & npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: npm install failed!" -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
    Write-Host "  Dependencies installed" -ForegroundColor Green
} else {
    Write-Host "  Dependencies already installed" -ForegroundColor Gray
}

Write-Host "  Starting Vite development server..." -ForegroundColor Yellow
Write-Host "  Press Ctrl+C to stop" -ForegroundColor Gray
Write-Host ""

& npm run dev

Write-Host ""
Write-Host "Frontend stopped." -ForegroundColor Yellow