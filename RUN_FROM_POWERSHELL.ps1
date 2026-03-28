# Run this script from PowerShell to see all output
# Right-click -> Run with PowerShell
# Or open PowerShell and run: .\RUN_FROM_POWERSHELL.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PDF RAG Chatbot - PowerShell Launcher" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Loading credentials..." -ForegroundColor Yellow

# Load credentials
$configFile = "demo\src\main\resources\application-local.properties"

if (-not (Test-Path $configFile)) {
    Write-Host "ERROR: Configuration file not found!" -ForegroundColor Red
    Write-Host "Expected: $configFile" -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

$apiKey = (Get-Content $configFile | Select-String "nvidia.api.key=" | ForEach-Object { $_.ToString().Split('=')[1] })
$dbPassword = (Get-Content $configFile | Select-String "spring.datasource.password=" | ForEach-Object { $_.ToString().Split('=')[1] })

if (-not $apiKey) {
    Write-Host "ERROR: Could not load NVIDIA API key" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

if (-not $dbPassword) {
    Write-Host "ERROR: Could not load database password" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "Credentials loaded successfully" -ForegroundColor Green
Write-Host ""

# Set environment variables
$env:NVIDIA_API_KEY = $apiKey
$env:DB_PASSWORD = $dbPassword

Write-Host "Starting application..." -ForegroundColor Yellow
Write-Host "This may take 10-15 seconds..." -ForegroundColor Yellow
Write-Host ""
Write-Host "Press Ctrl+C to stop the application" -ForegroundColor Cyan
Write-Host ""

# Change to demo directory and run
Set-Location demo

try {
    & .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
} catch {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "ERROR: Application failed to start" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
}

Write-Host ""
Write-Host "Application stopped" -ForegroundColor Yellow
Read-Host "Press Enter to exit"
