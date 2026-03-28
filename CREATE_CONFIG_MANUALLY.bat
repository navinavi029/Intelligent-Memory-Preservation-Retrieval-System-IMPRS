@echo off
echo ========================================
echo Manual Configuration File Creator
echo ========================================
echo.
echo This will help you create the configuration file manually.
echo.
pause
echo.

set /p NVIDIA_API_KEY="Enter your NVIDIA API Key: "
echo.
set /p DB_PASSWORD="Enter your PostgreSQL password: "
echo.

echo Creating configuration file...
echo.

REM Create directory if it doesn't exist
if not exist "demo\src\main\resources" mkdir "demo\src\main\resources"

REM Create the file
(
echo nvidia.api.key=%NVIDIA_API_KEY%
echo spring.ai.openai.api-key=%NVIDIA_API_KEY%
echo spring.datasource.password=%DB_PASSWORD%
echo logging.level.root=INFO
echo logging.level.com.example.demo=DEBUG
echo logging.level.org.springframework.ai=DEBUG
) > "demo\src\main\resources\application-local.properties"

if exist "demo\src\main\resources\application-local.properties" (
    echo.
    echo SUCCESS! Configuration file created at:
    echo demo\src\main\resources\application-local.properties
    echo.
    echo You can now run START_APP.bat to start the application.
    echo.
) else (
    echo.
    echo ERROR: Failed to create file!
    echo.
    echo Please create it manually with these contents:
    echo.
    echo nvidia.api.key=%NVIDIA_API_KEY%
    echo spring.ai.openai.api-key=%NVIDIA_API_KEY%
    echo spring.datasource.password=%DB_PASSWORD%
    echo.
)

pause
