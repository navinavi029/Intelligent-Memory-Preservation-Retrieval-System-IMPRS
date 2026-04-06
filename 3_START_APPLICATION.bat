@echo off
echo ========================================
echo PDF RAG Chatbot - Starting Application
echo ========================================
echo.

REM Check if config file exists
if not exist "demo\src\main\resources\application-local.properties" (
    echo [ERROR] Configuration file not found!
    echo.
    echo Please run: 2_CONFIGURE_API_KEY.bat first
    echo.
    pause
    exit /b 1
)

echo [1/4] Checking Docker...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not installed or not running!
    echo Please run: 1_CHECK_PREREQUISITES.bat
    pause
    exit /b 1
)
echo [OK] Docker is available
echo.

echo [2/4] Checking PostgreSQL container...
docker ps -a --filter "name=rag-postgres" --format "{{.Names}}" | findstr /C:"rag-postgres" >nul 2>&1
if %errorlevel% equ 0 (
    echo [INFO] Container exists, checking status...
    docker ps --filter "name=rag-postgres" --format "{{.Names}}" | findstr /C:"rag-postgres" >nul 2>&1
    if %errorlevel% equ 0 (
        echo [OK] Container is already running
    ) else (
        echo [INFO] Starting existing container...
        docker start rag-postgres
        if %errorlevel% neq 0 (
            echo [ERROR] Failed to start existing container!
            pause
            exit /b 1
        )
        echo [OK] Container started
    )
) else (
    echo [INFO] Container does not exist, creating new one...
    docker-compose up -d
    if %errorlevel% neq 0 (
        echo [ERROR] Failed to create Docker container!
        echo Make sure Docker Desktop is running.
        pause
        exit /b 1
    )
    echo [OK] New container created and started
)
echo.

echo [3/4] Waiting for database to initialize...
echo This takes about 15 seconds...
timeout /t 15 /nobreak >nul

docker exec rag-postgres pg_isready -U raguser -d ragdb >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Database is ready!
) else (
    echo [WARNING] Database might still be initializing...
    echo Waiting 5 more seconds...
    timeout /t 5 /nobreak >nul
)
echo.

echo [3.5/4] Checking database schema...
docker exec rag-postgres psql -U raguser -d ragdb -c "\dt" | findstr "document_chunks" >nul 2>&1
if %errorlevel% neq 0 (
    echo [INFO] Schema not found, initializing database...
    powershell -Command "Get-Content demo\src\main\resources\schema.sql | docker exec -i rag-postgres psql -U raguser -d ragdb" >nul 2>&1
    if %errorlevel% equ 0 (
        echo [OK] Database schema initialized
    ) else (
        echo [ERROR] Failed to initialize database schema!
        pause
        exit /b 1
    )
) else (
    echo [OK] Database schema exists
)
echo.

echo [4/4] Starting Spring Boot application...
echo.
echo ========================================
echo Application will be available at:
echo   - Swagger UI: http://localhost:8080/swagger-ui/index.html
echo   - API Base:   http://localhost:8080/api
echo ========================================
echo.
echo Starting... (this may take 10-15 seconds)
echo.
echo Press Ctrl+C to stop the application
echo.

cd demo
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local

pause
