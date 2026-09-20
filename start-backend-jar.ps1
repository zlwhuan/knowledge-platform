# Start Backend using JAR file
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Set-Location "D:\Projects\knowledge-platform\backend"

Write-Host "Starting Backend using JAR file..." -ForegroundColor Green
Write-Host "JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Yellow

java -jar target\knowledge-platform-0.0.1-SNAPSHOT.jar