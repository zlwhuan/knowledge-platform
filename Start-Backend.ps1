# Start Backend with Maven Build
# 配置 LLM 环境变量 → 杀死现有进程 → Maven编译打包 → 启动后端

$ErrorActionPreference = "Stop"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# ---- LLM 配置（技能助手）----
$env:LLM_API_BASE_URL = "https://token-plan-cn.xiaomimimo.com/v1"
$env:LLM_MODEL = "mimo-v2.6-flash"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Knowledge Platform Backend Startup" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "LLM_API_BASE_URL = $env:LLM_API_BASE_URL" -ForegroundColor Gray
Write-Host "LLM_MODEL        = $env:LLM_MODEL" -ForegroundColor Gray

$env:LLM_API_KEY = Read-Host "请输入 LLM_API_KEY"
if ([string]::IsNullOrWhiteSpace($env:LLM_API_KEY)) {
    Write-Host "ERROR: LLM_API_KEY 不能为空（技能助手将无法调用模型）" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}
Write-Host "LLM_API_KEY      = ****（已设置）" -ForegroundColor Gray
Write-Host ""

Set-Location "D:\Projects\knowledge-platform\backend"

# 1. 杀掉占用 JAR 的后端进程
Write-Host "[1/3] Stopping existing backend processes..." -ForegroundColor Yellow
$javaProcesses = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue | Where-Object {
    $cmd = $_.CommandLine
    $cmd -and (
        $cmd -like "*knowledge-platform*" -or
        $cmd -like "*spring-boot*" -or
        ($cmd -like "*-jar*target*" -and $cmd -like "*.jar")
    )
}

if ($javaProcesses) {
    foreach ($proc in $javaProcesses) {
        Write-Host "  Stopping PID $($proc.ProcessId): $($proc.CommandLine)" -ForegroundColor Gray
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
    }
    Write-Host "  Stopped $($javaProcesses.Count) Java process(es)" -ForegroundColor Green
    Start-Sleep -Seconds 2
} else {
    Write-Host "  No existing backend processes found" -ForegroundColor Gray
}

# 2. Maven编译打包
Write-Host ""
Write-Host "[2/3] Building with Maven..." -ForegroundColor Yellow
Write-Host "  JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Gray

& .\mvnw.cmd clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Maven build failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "  Build successful!" -ForegroundColor Green
Write-Host "  JAR: target\knowledge-platform-0.0.1-SNAPSHOT.jar" -ForegroundColor Gray

# 3. 启动后端
Write-Host ""
Write-Host "[3/3] Starting Backend on port 8080..." -ForegroundColor Yellow
Write-Host "  Press Ctrl+C to stop" -ForegroundColor Gray
Write-Host ""

java -jar target\knowledge-platform-0.0.1-SNAPSHOT.jar

Write-Host ""
Write-Host "Backend stopped." -ForegroundColor Yellow