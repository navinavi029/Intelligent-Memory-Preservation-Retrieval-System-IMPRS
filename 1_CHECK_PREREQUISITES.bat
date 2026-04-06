@echo off
setlocal enabledelayedexpansion
echo ========================================
echo Prerequisites Checker and Installer
echo ========================================
echo.

set ALL_OK=1
set DOCKER_MISSING=0
set JAVA_MISSING=0

echo [1/3] Checking Docker...
docker --version >nul 2>&1
if errorlevel 1 goto DOCKER_NOT_INSTALLED

echo [OK] Docker is installed
docker --version

docker ps >nul 2>&1
if errorlevel 1 goto DOCKER_NOT_RUNNING

echo [OK] Docker is running
goto CHECK_JAVA

:DOCKER_NOT_RUNNING
echo [WARNING] Docker is installed but not running!
echo.
set /p START_DOCKER="Would you like to start Docker Desktop? (y/n): "
if /i "!START_DOCKER!"=="y" (
    echo Starting Docker Desktop...
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    echo.
    echo Waiting for Docker to start... (this may take 30-60 seconds)
    timeout /t 10 /nobreak >nul
    
    set DOCKER_WAIT=0
    :WAIT_DOCKER
    docker ps >nul 2>&1
    if not errorlevel 1 (
        echo [OK] Docker is now running!
        goto CHECK_JAVA
    )
    set /a DOCKER_WAIT+=1
    if !DOCKER_WAIT! lss 12 (
        echo Still waiting... (!DOCKER_WAIT!/12)
        timeout /t 5 /nobreak >nul
        goto WAIT_DOCKER
    )
    echo [ERROR] Docker failed to start in time
    echo        Please start Docker Desktop manually and try again
    set ALL_OK=0
) else (
    echo [ERROR] Docker needs to be running
    set ALL_OK=0
)
goto CHECK_JAVA

:DOCKER_NOT_INSTALLED
echo [ERROR] Docker is not installed!
set DOCKER_MISSING=1
set ALL_OK=0
echo.
set /p INSTALL_DOCKER="Would you like to download Docker Desktop installer? (y/n): "
if /i "!INSTALL_DOCKER!"=="y" (
    echo Opening Docker Desktop download page...
    start https://www.docker.com/products/docker-desktop
    echo.
    echo Please:
    echo   1. Download and install Docker Desktop
    echo   2. Restart your computer if prompted
    echo   3. Start Docker Desktop
    echo   4. Run this script again
)

:CHECK_JAVA
echo.
echo [2/3] Checking Java...
java -version >nul 2>&1
if errorlevel 1 goto JAVA_NOT_INSTALLED

echo [OK] Java is installed
java -version 2>&1 | findstr /C:"version"
echo [INFO] Java 17 or higher is required
goto CHECK_MAVEN

:JAVA_NOT_INSTALLED
echo [ERROR] Java is not installed!
set JAVA_MISSING=1
set ALL_OK=0
echo.
set /p INSTALL_JAVA="Would you like to download Java 17 installer? (y/n): "
if /i "!INSTALL_JAVA!"=="y" (
    echo Opening Java 17 download page...
    start https://adoptium.net/temurin/releases/?version=17
    echo.
    echo Please:
    echo   1. Download the Windows x64 MSI installer
    echo   2. Run the installer (accept all defaults)
    echo   3. Restart this command prompt
    echo   4. Run this script again
)

:CHECK_MAVEN
echo.
echo [3/3] Checking Maven wrapper...
if not exist "demo\mvnw.cmd" (
    echo [WARNING] Maven wrapper not found!
    echo.
    set /p INSTALL_MAVEN="Would you like to install Maven wrapper? (y/n): "
    if /i "!INSTALL_MAVEN!"=="y" (
        echo Installing Maven wrapper...
        cd demo
        
        REM Check if we have Maven installed globally
        mvn --version >nul 2>&1
        if not errorlevel 1 (
            echo Using global Maven to install wrapper...
            call mvn wrapper:wrapper
            cd ..
            if exist "demo\mvnw.cmd" (
                echo [OK] Maven wrapper installed successfully!
            ) else (
                echo [ERROR] Failed to install Maven wrapper
                set ALL_OK=0
            )
        ) else (
            echo [INFO] Maven is not installed globally
            echo.
            echo Option 1: Install Maven globally
            set /p INSTALL_GLOBAL_MAVEN="Would you like to download Maven installer? (y/n): "
            if /i "!INSTALL_GLOBAL_MAVEN!"=="y" (
                echo Opening Maven download page...
                start https://maven.apache.org/download.cgi
                echo.
                echo Please:
                echo   1. Download apache-maven-3.9.x-bin.zip
                echo   2. Extract to C:\Program Files\Maven
                echo   3. Add C:\Program Files\Maven\bin to PATH
                echo   4. Restart command prompt
                echo   5. Run this script again
                cd ..
                set ALL_OK=0
            ) else (
                echo.
                echo Option 2: Download complete project with Maven wrapper
                echo The Maven wrapper files should be part of the project.
                echo Please ensure you have the complete project source.
                cd ..
                set ALL_OK=0
            )
        )
    ) else (
        echo [ERROR] Maven wrapper is required to run the application
        cd ..
        set ALL_OK=0
    )
    goto FINAL_REPORT
)
echo [OK] Maven wrapper found (mvnw.cmd)

:FINAL_REPORT
echo.

echo ========================================
if !ALL_OK! equ 1 (
    echo [SUCCESS] All prerequisites are met!
    echo.
    echo Next steps:
    echo   1. Run: 2_CONFIGURE_API_KEY.bat
    echo   2. Run: 3_START_APPLICATION.bat
) else (
    echo [FAILED] Some prerequisites are missing.
    echo.
    if !DOCKER_MISSING! equ 1 (
        echo - Install Docker Desktop and restart your computer
    )
    if !JAVA_MISSING! equ 1 (
        echo - Install Java 17 and restart your command prompt
    )
    echo.
    echo After installing, run this script again to verify.
)
echo ========================================
echo.
pause
