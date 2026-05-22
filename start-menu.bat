@echo off
setlocal

:menu
cls
echo ============================================
echo     Knowledge Platform Startup Menu
echo ============================================
echo.
echo  1. Start Backend (Spring Boot)
echo  2. Start Frontend (Vite)
echo  3. Start Both (in separate windows)
echo  4. Start Nginx (Reverse Proxy)
echo  5. Start All (Backend + Frontend + Nginx)
echo  6. Stop All Services
echo  7. Exit
echo.
echo ============================================
set /p choice="Enter your choice (1-7): "

if "%choice%"=="1" goto start_backend
if "%choice%"=="2" goto start_frontend
if "%choice%"=="3" goto start_both
if "%choice%"=="4" goto start_nginx
if "%choice%"=="5" goto start_all
if "%choice%"=="6" goto stop_all
if "%choice%"=="7" goto exit
echo Invalid choice. Please try again.
timeout /t 2 >nul
goto menu

:start_backend
echo.
echo Starting Backend...
start "Knowledge Platform Backend" cmd /k "cd /d C:\Users\Administrator\Documents\Projects\Nexus\backend && call mvnw.cmd spring-boot:run"
echo Backend started in a new window.
timeout /t 3 >nul
goto menu

:start_frontend
echo.
echo Starting Frontend...
start "Knowledge Platform Frontend" cmd /k "cd /d C:\Users\Administrator\Documents\Projects\Nexus\frontend && call npm run dev"
echo Frontend started in a new window.
timeout /t 3 >nul
goto menu

:start_both
echo.
echo Starting Backend and Frontend in separate windows...
start "Knowledge Platform Backend" cmd /k "cd /d C:\Users\Administrator\Documents\Projects\Nexus\backend && call mvnw.cmd spring-boot:run"
timeout /t 2 >nul
start "Knowledge Platform Frontend" cmd /k "cd /d C:\Users\Administrator\Documents\Projects\Nexus\frontend && call npm run dev"
echo Both services started in separate windows.
timeout /t 3 >nul
goto menu

:start_nginx
echo.
echo Starting Nginx...
start "Nginx Server" cmd /k "cd /d C:\Users\Administrator\Documents\Projects\nginx-1.28.2 && .\start-nginx.bat"
echo Nginx started.
timeout /t 3 >nul
goto menu

:start_all
echo.
echo Starting All Services (Backend, Frontend, Nginx)...
start "Knowledge Platform Backend" cmd /k "cd /d C:\Users\Administrator\Documents\Projects\Nexus\backend && call mvnw.cmd spring-boot:run"
timeout /t 2 >nul
start "Knowledge Platform Frontend" cmd /k "cd /d C:\Users\Administrator\Documents\Projects\Nexus\frontend && call npm run dev"
timeout /t 3 >nul
start "Nginx Server" cmd /k "cd /d C:\Users\Administrator\Documents\Projects\nginx-1.28.2 && .\start-nginx.bat"
echo All services started.
echo.
echo Access URLs:
echo   Direct Frontend: http://localhost:5173/
echo   Direct Backend: http://localhost:8080/
echo   Via Nginx: http://192.168.1.142/
timeout /t 5 >nul
goto menu

:stop_all
echo.
echo Stopping all services...
taskkill /f /im java.exe >nul 2>&1
taskkill /f /im node.exe >nul 2>&1
taskkill /f /im nginx.exe >nul 2>&1
echo All services stopped.
timeout /t 3 >nul
goto menu

:exit
echo.
echo Goodbye!
timeout /t 2 >nul
exit