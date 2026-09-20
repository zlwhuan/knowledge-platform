# Test RAG Sync Functionality
Write-Host "Testing RAG Sync..." -ForegroundColor Cyan

# Test 1: Check RAG service
Write-Host ""
Write-Host "[1] Checking RAG Service..." -ForegroundColor Yellow
try {
    $ragStatus = Invoke-RestMethod -Uri "http://localhost:8081/api/rag/status" -Method Get
    Write-Host "  RAG Service: $($ragStatus.status)" -ForegroundColor Green
    Write-Host "  Total chunks: $($ragStatus.total_chunks)" -ForegroundColor White
} catch {
    Write-Host "  RAG Service: Not responding" -ForegroundColor Red
}

# Test 2: Manually sync a test item to RAG
Write-Host ""
Write-Host "[2] Testing manual RAG sync..." -ForegroundColor Yellow
$testItem = @{
    item_id = "test-999"
    title = "Test Knowledge Item"
    content = "This is a test knowledge item content for verifying RAG sync functionality."
    category = "Test Category"
    tags = @("test", "RAG")
    doc_type = "products"
    attachments = @()
} | ConvertTo-Json -Depth 3

try {
    $result = Invoke-RestMethod -Uri "http://localhost:8081/api/rag/sync" -Method Post -Body ([System.Text.Encoding]::UTF8.GetBytes($testItem)) -ContentType "application/json; charset=utf-8"
    Write-Host "  Sync result: $($result.status)" -ForegroundColor Green
    Write-Host "  Chunks created: $($result.chunks_created)" -ForegroundColor White
} catch {
    Write-Host "  Sync failed: $_" -ForegroundColor Red
}

# Test 3: Search for the test item
Write-Host ""
Write-Host "[3] Searching for test item..." -ForegroundColor Yellow
Start-Sleep -Seconds 2
$searchQuery = @{
    query = "Test Knowledge Item"
    top_k = 5
} | ConvertTo-Json

try {
    $searchResult = Invoke-RestMethod -Uri "http://localhost:8081/api/rag/search" -Method Post -Body ([System.Text.Encoding]::UTF8.GetBytes($searchQuery)) -ContentType "application/json; charset=utf-8"
    if ($searchResult.Count -gt 0) {
        Write-Host "  Found $($searchResult.Count) results" -ForegroundColor Green
        Write-Host "  First result: $($searchResult[0].title)" -ForegroundColor White
    } else {
        Write-Host "  No results found" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  Search failed: $_" -ForegroundColor Red
}

# Test 4: Check backend API
Write-Host ""
Write-Host "[4] Checking Backend API..." -ForegroundColor Yellow
try {
    $backendStatus = Invoke-RestMethod -Uri "http://localhost:8080/api/system/overview" -Method Get -TimeoutSec 5
    Write-Host "  Backend: Running" -ForegroundColor Green
} catch {
    Write-Host "  Backend: Not responding" -ForegroundColor Red
}

Write-Host ""
Write-Host "Test completed!" -ForegroundColor Cyan