# Start RAG API Server
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$py = Join-Path $root ".venv\Scripts\python.exe"
if (-not (Test-Path $py)) {
    Write-Error "venv not found: $py"
    exit 1
}
Push-Location $root
try {
    Write-Host "Starting RAG API Server on port 8081..." -ForegroundColor Green
    & $py -m uvicorn api_server:app --host 0.0.0.0 --port 8081 --reload @args
}
finally {
    Pop-Location
}