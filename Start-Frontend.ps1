# Start-Frontend.ps1
Write-Host "Starting Knowledge Platform Frontend..." -ForegroundColor Green
Write-Host ""

# 检查Node.js是否安装
try {
    $null = Get-Command node -ErrorAction Stop
    Write-Host "Node.js version:" -ForegroundColor Green
    node --version
    Write-Host ""
    
    Write-Host "npm version:" -ForegroundColor Green
    npm --version
    Write-Host ""
} catch {
    Write-Host "ERROR: Node.js not found. Please install Node.js." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# 检查是否已安装依赖
if (-not (Test-Path "node_modules")) {
    Write-Host "Installing dependencies..." -ForegroundColor Yellow
    & npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: npm install failed." -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
    Write-Host "Dependencies installed." -ForegroundColor Green
}

Write-Host ""
Write-Host "Starting Vite development server..." -ForegroundColor Green
Write-Host "Logs will appear below. Press Ctrl+C to stop." -ForegroundColor Yellow
Write-Host ""

# 运行前端开发服务器
& npm run dev

Write-Host ""
Write-Host "Frontend stopped." -ForegroundColor Yellow
Read-Host "Press Enter to exit"