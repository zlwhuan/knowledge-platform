# Start Backend with Maven Build
# 杀死现有进程、Maven编译打包、启动后端

$ErrorActionPreference = "Stop"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Set-Location "D:\Projects\knowledge-platform\backend"

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Knowledge Platform Backend Startup" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# 1. 杀死现有的Java进程（知识平台后端）
Write-Host "[1/3] Stopping existing backend processes..." -ForegroundColor Yellow
$javaProcesses = Get-Process java -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -like "*knowledge-platform*" -or 
    $_.CommandLine -like "*spring-boot*" -or
    $_.CommandLine -like "*.jar"
}

if ($javaProcesses) {
    $javaProcesses | Stop-Process -Force -ErrorAction SilentlyContinue
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