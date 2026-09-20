# Start Full Knowledge Platform with RAG Service
# This script starts all services: MySQL, Backend, RAG Service, and Frontend

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Knowledge Platform - Full Stack Startup" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Check prerequisites
Write-Host "Checking prerequisites..." -ForegroundColor Yellow

# Check Python
$pythonPath = Join-Path $root "product-assistant\.venv\Scripts\python.exe"
if (-not (Test-Path $pythonPath)) {
    Write-Host "Creating Python virtual environment..." -ForegroundColor Yellow
    Set-Location (Join-Path $root "product-assistant")
    python -m venv .venv
    & "$root\product-assistant\.venv\Scripts\pip.exe" install -r "$root\product-assistant\requirements.txt"
    & "$root\product-assistant\.venv\Scripts\pip.exe" install fastapi uvicorn pydantic python-multipart
}
Write-Host "Python: $pythonPath" -ForegroundColor Green

# Check Java
$javaPath = $null
$javaLocations = @(
    "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe",
    "C:\Program Files\Java\jdk-17*\bin\java.exe",
    "C:\Program Files\Java\jdk-21*\bin\java.exe"
)

foreach ($loc in $javaLocations) {
    $resolved = Resolve-Path $loc -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($resolved -and (Test-Path $resolved)) {
        $javaPath = $resolved.Path
        break
    }
}

if (-not $javaPath) {
    # Try to find java in PATH
    $javaInPath = Get-Command java -ErrorAction SilentlyContinue
    if ($javaInPath) {
        $javaPath = $javaInPath.Source
    }
}

if ($javaPath) {
    Write-Host "Java: $javaPath" -ForegroundColor Green
    $env:JAVA_HOME = Split-Path (Split-Path $javaPath)
} else {
    Write-Host "WARNING: Java not found in common locations. Maven wrapper will try to find it." -ForegroundColor Yellow
}

# Check Node.js
$nodePath = $null
$nodeLocations = @(
    "C:\Program Files\nodejs\node.exe",
    "$env:LOCALAPPDATA\Programs\nodejs\node.exe",
    "$env:APPDATA\nvm\*\node.exe"
)

foreach ($loc in $nodeLocations) {
    $resolved = Resolve-Path $loc -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($resolved -and (Test-Path $resolved)) {
        $nodePath = $resolved.Path
        break
    }
}

if (-not $nodePath) {
    $nodeInPath = Get-Command node -ErrorAction SilentlyContinue
    if ($nodeInPath) {
        $nodePath = $nodeInPath.Source
    }
}

if ($nodePath) {
    $nodeVersion = & $nodePath --version 2>&1
    Write-Host "Node.js: $nodeVersion ($nodePath)" -ForegroundColor Green
    # Add to PATH if not already there
    $nodeDir = Split-Path $nodePath
    if ($env:PATH -notlike "*$nodeDir*") {
        $env:PATH = "$nodeDir;$env:PATH"
    }
} else {
    Write-Host "ERROR: Node.js not found. Please install Node.js 18 or later." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Starting services..." -ForegroundColor Yellow
Write-Host ""

# Start RAG Service
Write-Host "[1/3] Starting RAG Service on port 8081..." -ForegroundColor Cyan
$ragServicePath = Join-Path $root "product-assistant"
$ragJob = Start-Job -ScriptBlock {
    param($path, $python)
    Set-Location $path
    & $python -m uvicorn api_server:app --host 0.0.0.0 --port 8081
} -ArgumentList $ragServicePath, $pythonPath

# Wait for RAG service to start
Start-Sleep -Seconds 3
Write-Host "  RAG Service started (Job ID: $($ragJob.Id))" -ForegroundColor Green

# Start Backend
Write-Host "[2/3] Starting Backend on port 8080..." -ForegroundColor Cyan
$backendPath = Join-Path $root "backend"
$backendJob = Start-Job -ScriptBlock {
    param($path)
    Set-Location $path
    & .\mvnw.cmd spring-boot:run
} -ArgumentList $backendPath

# Wait for backend to start
Start-Sleep -Seconds 5
Write-Host "  Backend started (Job ID: $($backendJob.Id))" -ForegroundColor Green

# Start Frontend
Write-Host "[3/3] Starting Frontend on port 5173..." -ForegroundColor Cyan
$frontendPath = Join-Path $root "frontend"
$frontendJob = Start-Job -ScriptBlock {
    param($path)
    Set-Location $path
    & npm run dev
} -ArgumentList $frontendPath

# Wait for frontend to start
Start-Sleep -Seconds 3
Write-Host "  Frontend started (Job ID: $($frontendJob.Id))" -ForegroundColor Green

Write-Host ""
Write-Host "======================================" -ForegroundColor Green
Write-Host "All services started successfully!" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Green
Write-Host ""
Write-Host "Services:" -ForegroundColor Yellow
Write-Host "  - Frontend:  http://localhost:5173" -ForegroundColor White
Write-Host "  - Backend:   http://localhost:8080" -ForegroundColor White
Write-Host "  - RAG API:   http://localhost:8081" -ForegroundColor White
Write-Host "  - Swagger:   http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host ""
Write-Host "Press Ctrl+C to stop all services" -ForegroundColor Yellow
Write-Host ""

# Keep script running and monitor jobs
try {
    while ($true) {
        # Check if any job failed
        $failedJobs = Get-Job | Where-Object { $_.State -eq 'Failed' }
        if ($failedJobs) {
            Write-Host "WARNING: Some services failed:" -ForegroundColor Red
            foreach ($job in $failedJobs) {
                Write-Host "  - $($job.Name): $($job.ChildJobs[0].JobStateInfo.Reason.Message)" -ForegroundColor Red
            }
        }
        
        Start-Sleep -Seconds 10
    }
} finally {
    # Cleanup on exit
    Write-Host ""
    Write-Host "Stopping all services..." -ForegroundColor Yellow
    
    Get-Job | Stop-Job
    Get-Job | Remove-Job -Force
    
    Write-Host "All services stopped." -ForegroundColor Green
}