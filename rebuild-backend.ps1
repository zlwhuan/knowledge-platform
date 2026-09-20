# Rebuild Backend
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Set-Location "D:\Projects\knowledge-platform\backend"

Write-Host "Rebuilding backend..." -ForegroundColor Green
Write-Host "JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Yellow

# Stop existing backend process if running
Write-Host "Stopping existing backend process..." -ForegroundColor Yellow
Get-Process java -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -like "*knowledge-platform*"
} | Stop-Process -Force -ErrorAction SilentlyContinue

# Build
Write-Host "Building with Maven..." -ForegroundColor Yellow
& .\mvnw.cmd clean package -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful!" -ForegroundColor Green
    Write-Host "JAR file: target\knowledge-platform-0.0.1-SNAPSHOT.jar" -ForegroundColor Cyan
} else {
    Write-Host "Build failed!" -ForegroundColor Red
}