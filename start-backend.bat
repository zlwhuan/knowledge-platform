@echo off
setlocal

echo Starting Knowledge Platform Backend...
echo.

REM 设置JAVA_HOME环境变量（如果未设置）
if "%JAVA_HOME%"=="" (
    echo JAVA_HOME not set, attempting to find Java...
    where java >nul 2>nul
    if %errorlevel% equ 0 (
        echo Java found in PATH, using system Java.
    ) else (
        echo ERROR: Java not found in PATH. Please install Java or set JAVA_HOME.
        pause
        exit /b 1
    )
) else (
    echo Using JAVA_HOME: %JAVA_HOME%
)

REM 检查并构建项目（如果需要）
if not exist "target\knowledge-platform-0.0.1-SNAPSHOT.jar" (
    echo Building project with Maven...
    call mvnw.cmd clean package -DskipTests
    if %errorlevel% neq 0 (
        echo ERROR: Build failed.
        pause
        exit /b 1
    )
    echo Build completed.
)

echo.
echo Starting Spring Boot application...
echo Logs will appear below. Press Ctrl+C to stop.
echo.

REM 运行Spring Boot应用
java -jar target\knowledge-platform-0.0.1-SNAPSHOT.jar

echo.
echo Backend stopped.
pause