@echo off
echo ========================================
echo PDF RAG Chatbot - Starting Application
echo ========================================
echo.

REM Check if config file exists
if not exist "demo\src\main\resources\application-local.properties" (
    echo ERROR: Configuration file not found!
    echo.
    echo Please run CREATE_CONFIG_MANUALLY.bat first to set up your credentials.
    echo.
    pause
    exit /b 1
)

echo Loading credentials...
cd demo
echo.
echo Starting application...
echo This may take 10-15 seconds...
echo.
echo Press Ctrl+C to stop the application
echo.

REM Run the application
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local

pause
