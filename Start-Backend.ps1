# Start-Backend.ps1
Write-Host "Starting Knowledge Platform Backend..." -ForegroundColor Green
Write-Host ""

# 设置JAVA_HOME环境变量（如果未设置）
if (-not $env:JAVA_HOME) {
    Write-Host "JAVA_HOME not set, attempting to find Java..." -ForegroundColor Yellow
    try {
        $null = Get-Command java -ErrorAction Stop
        Write-Host "Java found in PATH, using system Java." -ForegroundColor Green
    } catch {
        Write-Host "ERROR: Java not found in PATH. Please install Java or set JAVA_HOME." -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
} else {
    Write-Host "Using JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green
}

# 检查并构建项目（如果需要）
if (-not (Test-Path "target\knowledge-platform-0.0.1-SNAPSHOT.jar")) {
    Write-Host "Building project with Maven..." -ForegroundColor Yellow
    & .\mvnw.cmd clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Build failed." -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
    Write-Host "Build completed." -ForegroundColor Green
}

Write-Host ""
Write-Host "Starting Spring Boot application..." -ForegroundColor Green
Write-Host "Logs will appear below. Press Ctrl+C to stop." -ForegroundColor Yellow
Write-Host ""

# 运行Spring Boot应用
java -jar target\knowledge-platform-0.0.1-SNAPSHOT.jar

Write-Host ""
Write-Host "Backend stopped." -ForegroundColor Yellow
Read-Host "Press Enter to exit"