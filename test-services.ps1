# Test all services
Write-Host "Testing Knowledge Platform Services..." -ForegroundColor Cyan
Write-Host ""

# Test RAG Service
Write-Host "[1] Testing RAG Service (port 8081)..." -ForegroundColor Yellow
try {
    $ragStatus = Invoke-RestMethod -Uri "http://localhost:8081/health" -Method Get -TimeoutSec 5
    Write-Host "  ✓ RAG Service: $($ragStatus.status)" -ForegroundColor Green
} catch {
    Write-Host "  ✗ RAG Service: Not responding" -ForegroundColor Red
}

# Test Backend
Write-Host "[2] Testing Backend (port 8080)..." -ForegroundColor Yellow
try {
    $backendStatus = Invoke-RestMethod -Uri "http://localhost:8080/api/system/overview" -Method Get -TimeoutSec 5
    Write-Host "  ✓ Backend: Running" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Backend: Not responding" -ForegroundColor Red
}

# Test Frontend
Write-Host "[3] Testing Frontend (port 5173)..." -ForegroundColor Yellow
try {
    $frontendResponse = Invoke-WebRequest -Uri "http://localhost:5173" -Method Get -TimeoutSec 5 -UseBasicParsing
    if ($frontendResponse.StatusCode -eq 200) {
        Write-Host "  ✓ Frontend: Running" -ForegroundColor Green
    }
} catch {
    Write-Host "  ✗ Frontend: Not responding" -ForegroundColor Red
}

Write-Host ""
Write-Host "Service URLs:" -ForegroundColor Cyan
Write-Host "  - Frontend:  http://localhost:5173" -ForegroundColor White
Write-Host "  - Backend:   http://localhost:8080" -ForegroundColor White
Write-Host "  - RAG API:   http://localhost:8081" -ForegroundColor White
Write-Host "  - Swagger:   http://localhost:8080/swagger-ui.html" -ForegroundColor White