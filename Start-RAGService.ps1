# Start RAG Service
# 检查环境 → 结束占用进程 → 启动 RAG (8081)，日志留在当前窗口
# 编码：UTF-8 with BOM

$ErrorActionPreference = "Continue"
$root = $PSScriptRoot
if (-not $root) { $root = Get-Location }
$ragServicePath = Join-Path $root "product-assistant"
$pythonPath = Join-Path $ragServicePath ".venv\Scripts\python.exe"

function Pause-Exit([int]$code = 0) {
    Write-Host ""
    Read-Host "Press Enter to close"
    exit $code
}

try {
    Write-Host "======================================" -ForegroundColor Cyan
    Write-Host "Knowledge Platform RAG Service Startup" -ForegroundColor Cyan
    Write-Host "======================================" -ForegroundColor Cyan
    Write-Host ""

    # 1. Python venv
    Write-Host "[1/3] Checking Python environment..." -ForegroundColor Yellow
    if (-not (Test-Path $pythonPath)) {
        Write-Host "  venv not found: $pythonPath" -ForegroundColor Yellow
        Write-Host "  Creating virtual environment..." -ForegroundColor Yellow
        Push-Location $ragServicePath
        python -m venv .venv
        Pop-Location
        if (-not (Test-Path $pythonPath)) {
            Write-Host "ERROR: Failed to create venv" -ForegroundColor Red
            Pause-Exit 1
        }
        Write-Host "  Installing dependencies (this may take minutes)..." -ForegroundColor Yellow
        & $pythonPath -m pip install -r (Join-Path $ragServicePath "requirements.txt")
        if ($LASTEXITCODE -ne 0) {
            Write-Host "ERROR: pip install failed" -ForegroundColor Red
            Pause-Exit 1
        }
    }
    Write-Host "  Python: $pythonPath" -ForegroundColor Green
    & $pythonPath --version

    # 2. 结束占用 8081 / 重复 uvicorn 的进程
    Write-Host ""
    Write-Host "[2/3] Stopping existing RAG processes..." -ForegroundColor Yellow
    $targets = Get-CimInstance Win32_Process -Filter "Name='python.exe' OR Name='pythonw.exe'" -ErrorAction SilentlyContinue | Where-Object {
        $cmd = $_.CommandLine
        $cmd -and ($cmd -like "*uvicorn*" -or $cmd -like "*api_server*")
    }
    if ($targets) {
        foreach ($p in $targets) {
            Write-Host "  Stopping PID $($p.ProcessId)" -ForegroundColor Gray
            Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
        }
        Start-Sleep -Seconds 2
    } else {
        Write-Host "  None found" -ForegroundColor Gray
    }

    # 3. 启动（前台，日志打印在本窗口）
    Write-Host ""
    Write-Host "[3/3] Starting RAG Service on port 8081..." -ForegroundColor Yellow
    Write-Host "  API : http://localhost:8081" -ForegroundColor Gray
    Write-Host "  Stop: Ctrl+C" -ForegroundColor Gray
    Write-Host ""

    Push-Location $ragServicePath
    & $pythonPath -m uvicorn api_server:app --host 0.0.0.0 --port 8081
    Pop-Location

    Write-Host ""
    Write-Host "RAG Service exited." -ForegroundColor Yellow
    Pause-Exit 0
} catch {
    Write-Host ""
    Write-Host "FATAL: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host $_.ScriptStackTrace -ForegroundColor DarkGray
    Pause-Exit 1
}
