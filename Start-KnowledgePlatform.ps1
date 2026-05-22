# Knowledge Platform Startup Script
# PowerShell version with better error handling and colored output

function Show-Menu {
    Clear-Host
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "     Knowledge Platform Startup Menu" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  1. Start Backend (Spring Boot)" -ForegroundColor Green
    Write-Host "  2. Start Frontend (Vite)" -ForegroundColor Green
    Write-Host "  3. Start Both (in separate windows)" -ForegroundColor Green
    Write-Host "  4. Start Nginx (Reverse Proxy)" -ForegroundColor Green
    Write-Host "  5. Start All (Backend + Frontend + Nginx)" -ForegroundColor Green
    Write-Host "  6. Stop All Services" -ForegroundColor Red
    Write-Host "  7. Check Service Status" -ForegroundColor Yellow
    Write-Host "  8. Exit" -ForegroundColor Gray
    Write-Host ""
    Write-Host "============================================" -ForegroundColor Cyan
}

function Start-Backend {
    Write-Host "`nStarting Backend..." -ForegroundColor Yellow
    try {
        Start-Process -FilePath "cmd.exe" -ArgumentList "/k `"cd /d C:\Users\Administrator\Documents\Projects\Nexus\backend && set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot && call mvnw.cmd spring-boot:run`"" -WindowStyle Normal
        Write-Host "Backend started in a new window." -ForegroundColor Green
    } catch {
        Write-Host "ERROR: Failed to start backend." -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor Red
    }
    Start-Sleep -Seconds 3
}

function Start-Frontend {
    Write-Host "`nStarting Frontend..." -ForegroundColor Yellow
    try {
        Start-Process -FilePath "cmd.exe" -ArgumentList "/k `"cd /d C:\Users\Administrator\Documents\Projects\Nexus\frontend && call npm run dev`"" -WindowStyle Normal
        Write-Host "Frontend started in a new window." -ForegroundColor Green
    } catch {
        Write-Host "ERROR: Failed to start frontend." -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor Red
    }
    Start-Sleep -Seconds 3
}

function Start-Both {
    Write-Host "`nStarting Backend and Frontend in separate windows..." -ForegroundColor Yellow
    Start-Backend
    Start-Sleep -Seconds 2
    Start-Frontend
    Write-Host "Both services started in separate windows." -ForegroundColor Green
    Start-Sleep -Seconds 3
}

function Start-Nginx {
    Write-Host "`nStarting Nginx..." -ForegroundColor Yellow
    try {
        $nginxPath = "C:\Users\Administrator\Documents\Projects\nginx-1.28.2\start-nginx.bat"
        if (Test-Path $nginxPath) {
            Start-Process -FilePath "cmd.exe" -ArgumentList "/k `"cd /d C:\Users\Administrator\Documents\Projects\nginx-1.28.2 && start-nginx.bat`"" -WindowStyle Normal
            Write-Host "Nginx started." -ForegroundColor Green
        } else {
            Write-Host "ERROR: Nginx startup script not found." -ForegroundColor Red
        }
    } catch {
        Write-Host "ERROR: Failed to start Nginx." -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor Red
    }
    Start-Sleep -Seconds 3
}

function Start-All {
    Write-Host "`nStarting All Services (Backend, Frontend, Nginx)..." -ForegroundColor Yellow
    Start-Backend
    Start-Sleep -Seconds 2
    Start-Frontend
    Start-Sleep -Seconds 3
    Start-Nginx
    Write-Host "All services started." -ForegroundColor Green
    Write-Host "`nAccess URLs:" -ForegroundColor Cyan
    Write-Host "  Direct Frontend: http://localhost:5173/" -ForegroundColor Gray
    Write-Host "  Direct Backend: http://localhost:8080/" -ForegroundColor Gray
    Write-Host "  Via Nginx: http://192.168.1.142/" -ForegroundColor Gray
    Start-Sleep -Seconds 5
}

function Stop-AllServices {
    Write-Host "`nStopping all services..." -ForegroundColor Red
    
    # Stop Java processes
    $javaProcesses = Get-Process -Name java -ErrorAction SilentlyContinue
    if ($javaProcesses) {
        $javaProcesses | Stop-Process -Force
        Write-Host "Stopped $($javaProcesses.Count) Java process(es)." -ForegroundColor Green
    } else {
        Write-Host "No Java processes found." -ForegroundColor Gray
    }
    
    # Stop Node.js processes
    $nodeProcesses = Get-Process -Name node -ErrorAction SilentlyContinue
    if ($nodeProcesses) {
        $nodeProcesses | Stop-Process -Force
        Write-Host "Stopped $($nodeProcesses.Count) Node.js process(es)." -ForegroundColor Green
    } else {
        Write-Host "No Node.js processes found." -ForegroundColor Gray
    }
    
    # Stop Nginx processes
    $nginxProcesses = Get-Process -Name nginx -ErrorAction SilentlyContinue
    if ($nginxProcesses) {
        $nginxProcesses | Stop-Process -Force
        Write-Host "Stopped $($nginxProcesses.Count) Nginx process(es)." -ForegroundColor Green
    } else {
        Write-Host "No Nginx processes found." -ForegroundColor Gray
    }
    
    Start-Sleep -Seconds 3
}

function Get-ServiceStatus {
    Write-Host "`nService Status:" -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan
    
    # Check Java processes
    $javaProcesses = Get-Process -Name java -ErrorAction SilentlyContinue
    if ($javaProcesses) {
        Write-Host "Backend (Java): RUNNING" -ForegroundColor Green
        foreach ($proc in $javaProcesses) {
            Write-Host "  PID: $($proc.Id), CPU: $($proc.CPU)s, Memory: $([math]::Round($proc.WorkingSet/1MB, 2)) MB" -ForegroundColor Gray
        }
    } else {
        Write-Host "Backend (Java): STOPPED" -ForegroundColor Red
    }
    
    # Check Node.js processes
    $nodeProcesses = Get-Process -Name node -ErrorAction SilentlyContinue
    if ($nodeProcesses) {
        Write-Host "Frontend (Node.js): RUNNING" -ForegroundColor Green
        foreach ($proc in $nodeProcesses) {
            Write-Host "  PID: $($proc.Id), CPU: $($proc.CPU)s, Memory: $([math]::Round($proc.WorkingSet/1MB, 2)) MB" -ForegroundColor Gray
        }
    } else {
        Write-Host "Frontend (Node.js): STOPPED" -ForegroundColor Red
    }
    
    # Check Nginx processes
    $nginxProcesses = Get-Process -Name nginx -ErrorAction SilentlyContinue
    if ($nginxProcesses) {
        Write-Host "Nginx: RUNNING" -ForegroundColor Green
        foreach ($proc in $nginxProcesses) {
            Write-Host "  PID: $($proc.Id), CPU: $($proc.CPU)s, Memory: $([math]::Round($proc.WorkingSet/1MB, 2)) MB" -ForegroundColor Gray
        }
    } else {
        Write-Host "Nginx: STOPPED" -ForegroundColor Red
    }
    
    # Check ports
    Write-Host "`nPort Status:" -ForegroundColor Cyan
    try {
        $backendPort = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
        if ($backendPort) {
            Write-Host "Port 8080 (Backend): LISTENING" -ForegroundColor Green
        } else {
            Write-Host "Port 8080 (Backend): NOT LISTENING" -ForegroundColor Red
        }
    } catch {
        Write-Host "Port 8080 (Backend): UNKNOWN" -ForegroundColor Yellow
    }
    
    try {
        $frontendPort = Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue
        if ($frontendPort) {
            Write-Host "Port 5173 (Frontend): LISTENING" -ForegroundColor Green
        } else {
            Write-Host "Port 5173 (Frontend): NOT LISTENING" -ForegroundColor Red
        }
    } catch {
        Write-Host "Port 5173 (Frontend): UNKNOWN" -ForegroundColor Yellow
    }
    
    try {
        $nginxPort = Get-NetTCPConnection -LocalPort 80 -ErrorAction SilentlyContinue
        if ($nginxPort) {
            Write-Host "Port 80 (Nginx): LISTENING" -ForegroundColor Green
        } else {
            Write-Host "Port 80 (Nginx): NOT LISTENING" -ForegroundColor Red
        }
    } catch {
        Write-Host "Port 80 (Nginx): UNKNOWN" -ForegroundColor Yellow
    }
    
    Write-Host "============================================" -ForegroundColor Cyan
    Start-Sleep -Seconds 5
}

# Main script
Write-Host "Knowledge Platform Startup Script" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

do {
    Show-Menu
    $choice = Read-Host "Enter your choice (1-6)"
    
    switch ($choice) {
        "1" { Start-Backend }
        "2" { Start-Frontend }
        "3" { Start-Both }
        "4" { Start-Nginx }
        "5" { Start-All }
        "6" { Stop-AllServices }
        "7" { Get-ServiceStatus }
        "8" { 
            Write-Host "`nGoodbye!" -ForegroundColor Cyan
            Start-Sleep -Seconds 2
            exit 
        }
        default {
            Write-Host "Invalid choice. Please try again." -ForegroundColor Red
            Start-Sleep -Seconds 2
        }
    }
} while ($true)