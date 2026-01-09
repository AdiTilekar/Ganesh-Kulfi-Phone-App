# Start Ganesh Kulfi Backend Locally
# Loads environment variables from .env and starts the server

Write-Host "🚀 Starting Ganesh Kulfi Backend..." -ForegroundColor Cyan
Write-Host ""

# Check if .env exists
if (-not (Test-Path ".env")) {
    Write-Host "❌ .env file not found!" -ForegroundColor Red
    Write-Host "   Run setup first: .\setup-local.ps1" -ForegroundColor Yellow
    exit 1
}

# Load environment variables from .env
Write-Host "📝 Loading configuration from .env..." -ForegroundColor Yellow
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        $key = $matches[1].Trim()
        $value = $matches[2].Trim()
        if (-not $key.StartsWith('#') -and -not [string]::IsNullOrWhiteSpace($key)) {
            [Environment]::SetEnvironmentVariable($key, $value, 'Process')
            Write-Host "   ✅ $key" -ForegroundColor Green
        }
    }
}
Write-Host ""

# Check if JAR exists
if (-not (Test-Path "build\libs\ganeshkulfi-backend-all.jar")) {
    Write-Host "❌ Application not built!" -ForegroundColor Red
    Write-Host "   Building now..." -ForegroundColor Yellow
    Write-Host ""
    .\gradlew.bat shadowJar
    Write-Host ""
}

# Start the backend
Write-Host "🌐 Starting server on port $env:APP_PORT..." -ForegroundColor Cyan
Write-Host "   Web UI: http://localhost:$env:APP_PORT" -ForegroundColor Yellow
Write-Host "   Health: http://localhost:$env:APP_PORT/health" -ForegroundColor Yellow
Write-Host ""
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Gray
Write-Host ""

java -Xms256m -Xmx512m -XX:+UseG1GC -jar build\libs\ganeshkulfi-backend-all.jar
