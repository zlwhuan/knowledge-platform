# Start Backend in Background
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Set-Location "D:\Projects\knowledge-platform\backend"

Write-Host "Starting Backend in background..." -ForegroundColor Green
Write-Host "JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Yellow

# Start Java process in background
$process = Start-Process -FilePath "java" -ArgumentList "-jar", "target\knowledge-platform-0.0.1-SNAPSHOT.jar" -WindowStyle Hidden -PassThru

Write-Host "Backend started with PID: $($process.Id)" -ForegroundColor Cyan
Write-Host "Waiting for startup..." -ForegroundColor Yellow

# Wait for startup
$maxWait = 60
$waited = 0
while ($waited -lt $maxWait) {
    Start-Sleep -Seconds 2
    $waited += 2
    
    # Check if port 8080 is listening
    $listening = netstat -ano | Select-String ":8080" | Select-String "LISTENING"
    if ($listening) {
        Write-Host "Backend is ready on port 8080!" -ForegroundColor Green
        break
    }
    
    Write-Host "Waiting... ($waited seconds)" -ForegroundColor Gray
}

if ($waited -ge $maxWait) {
    Write-Host "WARNING: Backend may not have started properly" -ForegroundColor Red
}