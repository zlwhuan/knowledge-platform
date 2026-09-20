# Start RAG Service Only
$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$ragServicePath = Join-Path $root "product-assistant"
$pythonPath = Join-Path $ragServicePath ".venv\Scripts\python.exe"

if (-not (Test-Path $pythonPath)) {
    Write-Host "Python venv not found. Creating..." -ForegroundColor Yellow
    Set-Location $ragServicePath
    python -m venv .venv
    .venv\Scripts\pip install -r requirements.txt
}

Write-Host "Starting RAG Service on port 8081..." -ForegroundColor Cyan
Set-Location $ragServicePath
& $pythonPath -m uvicorn api_server:app --host 0.0.0.0 --port 8081 --reload