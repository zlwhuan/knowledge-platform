@echo off
echo ======================================
echo Knowledge Platform - Service Launcher
echo ======================================
echo.

echo Starting RAG Service on port 8081...
start "RAG Service" cmd /k "cd /d D:\Projects\knowledge-platform\product-assistant && .\.venv\Scripts\python.exe -m uvicorn api_server:app --host 0.0.0.0 --port 8081"

timeout /t 3 /nobreak >nul

echo Starting Backend on port 8080...
start "Backend" cmd /k "cd /d D:\Projects\knowledge-platform\backend && .\mvnw.cmd spring-boot:run"

timeout /t 5 /nobreak >nul

echo Starting Frontend on port 5173...
start "Frontend" cmd /k "cd /d D:\Projects\knowledge-platform\frontend && npm run dev"

echo.
echo ======================================
echo All services starting!
echo ======================================
echo.
echo Services:
echo   - Frontend:  http://localhost:5173
echo   - Backend:   http://localhost:8080
echo   - RAG API:   http://localhost:8081
echo   - Swagger:   http://localhost:8080/swagger-ui.html
echo.
echo Press any key to close this window...
pause >nul