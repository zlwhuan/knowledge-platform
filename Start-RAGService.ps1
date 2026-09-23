# Start RAG Service
# 杀死现有进程、检查环境、启动RAG服务

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$ragServicePath = Join-Path $root "product-assistant"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Knowledge Platform RAG Service Startup" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# 1. 检查Python环境
Write-Host "[1/3] Checking Python environment..." -ForegroundColor Yellow
$pythonPath = Join-Path $ragServicePath ".venv\Scripts\python.exe"

if (-not (Test-Path $pythonPath)) {
    Write-Host "  Python virtual environment not found" -ForegroundColor Yellow
    Write-Host "  Creating virtual environment..." -ForegroundColor Yellow
    Set-Location $ragServicePath
    python -m venv .venv
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Failed to create Python virtual environment!" -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
    
    Write-Host "  Installing dependencies..." -ForegroundColor Yellow
    & $pythonPath -m pip install -r requirements.txt
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Failed to install Python dependencies!" -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
    Write-Host "  Virtual environment created and dependencies installed" -ForegroundColor Green
} else {
    Write-Host "  Python virtual environment found: $pythonPath" -ForegroundColor Green
}

# 2. 杀死现有的Python/uvicorn进程
Write-Host ""
Write-Host "[2/3] Stopping existing RAG service processes..." -ForegroundColor Yellow
$pythonProcesses = Get-Process python -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -like "*uvicorn*" -or 
    $_.CommandLine -like "*api_server*" -or
    $_.CommandLine -like "*product-assistant*"
}

if ($pythonProcesses) {
    $pythonProcesses | Stop-Process -Force -ErrorAction SilentlyContinue
    Write-Host "  Stopped $($pythonProcesses.Count) Python process(es)" -ForegroundColor Green
    Start-Sleep -Seconds 2
} else {
    Write-Host "  No existing RAG service processes found" -ForegroundColor Gray
}

# 3. 启动RAG服务
Write-Host ""
Write-Host "[3/3] Starting RAG Service on port 8081..." -ForegroundColor Yellow
Write-Host "  API: http://localhost:8081" -ForegroundColor Gray
Write-Host "  Press Ctrl+C to stop" -ForegroundColor Gray
Write-Host ""

Set-Location $ragServicePath
& $pythonPath -m uvicorn api_server:app --host 0.0.0.0 --port 8081 --reload

Write-Host ""
Write-Host "RAG Service stopped." -ForegroundColor Yellow