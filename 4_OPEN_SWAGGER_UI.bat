@echo off
echo ========================================
echo Opening Swagger UI
echo ========================================
echo.

echo Checking if application is running...
echo.

REM Try to check if the application is responding
curl -s http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Application is running!
    echo Opening Swagger UI in your default browser...
    echo.
    start http://localhost:8080/swagger-ui/index.html
    timeout /t 2 /nobreak >nul
    exit /b 0
)

REM If curl is not available or app is not running, just try to open it
echo [INFO] Cannot verify if application is running
echo Opening Swagger UI anyway...
echo.
echo If the page doesn't load, make sure to:
echo   1. Run: 3_START_APPLICATION.bat
echo   2. Wait for "Started DemoApplication" message
echo   3. Try again
echo.

start http://localhost:8080/swagger-ui/index.html

timeout /t 3 /nobreak >nul
