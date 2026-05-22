@echo off
setlocal

echo Starting Knowledge Platform Frontend...
echo.

REM 检查Node.js是否安装
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Node.js not found. Please install Node.js.
    pause
    exit /b 1
)

echo Node.js version:
node --version
echo.

echo npm version:
npm --version
echo.

REM 检查是否已安装依赖
if not exist "node_modules" (
    echo Installing dependencies...
    call npm install
    if %errorlevel% neq 0 (
        echo ERROR: npm install failed.
        pause
        exit /b 1
    )
    echo Dependencies installed.
)

echo.
echo Starting Vite development server...
echo Logs will appear below. Press Ctrl+C to stop.
echo.

REM 运行前端开发服务器
call npm run dev

echo.
echo Frontend stopped.
pause