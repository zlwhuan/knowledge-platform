# Start Backend with JAVA_HOME
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Set-Location "D:\Projects\knowledge-platform\backend"

Write-Host "Starting Backend with JAVA_HOME=$env:JAVA_HOME" -ForegroundColor Green
Write-Host "Java version:" -ForegroundColor Yellow
& java -version

Write-Host ""
Write-Host "Starting Spring Boot..." -ForegroundColor Yellow
& .\mvnw.cmd spring-boot:run