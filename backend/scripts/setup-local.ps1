# Local Testing Setup Script (Without Docker)
# Sets up local PostgreSQL database for testing

Write-Host "🚀 Ganesh Kulfi Backend - Local Setup" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Check PostgreSQL is running
Write-Host "📊 Checking PostgreSQL service..." -ForegroundColor Yellow
$pgService = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue | Where-Object {$_.Status -eq 'Running'}

if (-not $pgService) {
    Write-Host "❌ PostgreSQL is not running!" -ForegroundColor Red
    Write-Host "   Please start PostgreSQL service first." -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ PostgreSQL is running" -ForegroundColor Green
Write-Host ""

# Database credentials prompt
Write-Host "📝 Database Configuration" -ForegroundColor Cyan
Write-Host "   Default database: ganeshkulfi_db" -ForegroundColor Gray
Write-Host "   Default user: ganeshkulfi_user" -ForegroundColor Gray
Write-Host ""

$dbHost = Read-Host "Database host (default: localhost)"
if ([string]::IsNullOrWhiteSpace($dbHost)) { $dbHost = "localhost" }

$dbPort = Read-Host "Database port (default: 5432)"
if ([string]::IsNullOrWhiteSpace($dbPort)) { $dbPort = "5432" }

$dbName = Read-Host "Database name (default: ganeshkulfi_db)"
if ([string]::IsNullOrWhiteSpace($dbName)) { $dbName = "ganeshkulfi_db" }

$dbUser = Read-Host "Database user (default: ganeshkulfi_user)"
if ([string]::IsNullOrWhiteSpace($dbUser)) { $dbUser = "ganeshkulfi_user" }

$dbPassword = Read-Host "Database password" -AsSecureString
$dbPasswordPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($dbPassword)
)

if ([string]::IsNullOrWhiteSpace($dbPasswordPlain)) {
    Write-Host "❌ Password cannot be empty!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🔐 Generating JWT secret..." -ForegroundColor Yellow
$jwtSecret = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 64 | % {[char]$_})
Write-Host "✅ JWT secret generated" -ForegroundColor Green
Write-Host ""

# Create .env file
Write-Host "📝 Creating .env file..." -ForegroundColor Yellow
$envContent = @"
# Ganesh Kulfi Backend - Local Testing Configuration
# Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

# Database Configuration
DB_URL=jdbc:postgresql://${dbHost}:${dbPort}/${dbName}
DB_USER=$dbUser
DB_PASSWORD=$dbPasswordPlain
DB_POOL_SIZE=10

# JWT Configuration
JWT_SECRET=$jwtSecret
JWT_ISSUER=ganeshkulfi
JWT_AUDIENCE=ganeshkulfi-app

# Application Configuration
APP_PORT=8080
UPLOADS_DIR=./uploads
"@

$envContent | Out-File -FilePath ".env" -Encoding UTF8 -NoNewline
Write-Host "✅ .env file created" -ForegroundColor Green
Write-Host ""

# Create uploads directory
Write-Host "📁 Creating uploads directory..." -ForegroundColor Yellow
if (-not (Test-Path "uploads")) {
    New-Item -ItemType Directory -Path "uploads" | Out-Null
    Write-Host "✅ Uploads directory created" -ForegroundColor Green
} else {
    Write-Host "✅ Uploads directory already exists" -ForegroundColor Green
}
Write-Host ""

# Set environment variables for current session
Write-Host "🔧 Setting environment variables..." -ForegroundColor Yellow
$env:DB_URL = "jdbc:postgresql://${dbHost}:${dbPort}/${dbName}"
$env:DB_USER = $dbUser
$env:DB_PASSWORD = $dbPasswordPlain
$env:DB_POOL_SIZE = "10"
$env:JWT_SECRET = $jwtSecret
$env:JWT_ISSUER = "ganeshkulfi"
$env:JWT_AUDIENCE = "ganeshkulfi-app"
$env:APP_PORT = "8080"
$env:UPLOADS_DIR = "./uploads"
Write-Host "✅ Environment variables set" -ForegroundColor Green
Write-Host ""

# Check if JAR exists
Write-Host "📦 Checking if application is built..." -ForegroundColor Yellow
if (-not (Test-Path "build\libs\ganeshkulfi-backend-all.jar")) {
    Write-Host "⚠️  JAR file not found. Building now..." -ForegroundColor Yellow
    Write-Host ""
    .\gradlew.bat clean shadowJar
    Write-Host ""
    if (-not (Test-Path "build\libs\ganeshkulfi-backend-all.jar")) {
        Write-Host "❌ Build failed!" -ForegroundColor Red
        exit 1
    }
}
Write-Host "✅ Application built" -ForegroundColor Green
Write-Host ""

# Summary
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "✅ Setup Complete!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📊 Configuration:" -ForegroundColor Cyan
Write-Host "   Database: $dbName" -ForegroundColor White
Write-Host "   Host: ${dbHost}:${dbPort}" -ForegroundColor White
Write-Host "   User: $dbUser" -ForegroundColor White
Write-Host "   Port: 8080" -ForegroundColor White
Write-Host ""
Write-Host "🚀 To start the backend:" -ForegroundColor Cyan
Write-Host "   java -jar build\libs\ganeshkulfi-backend-all.jar" -ForegroundColor Yellow
Write-Host ""
Write-Host "Or use the start script:" -ForegroundColor Cyan
Write-Host "   .\start-backend.ps1" -ForegroundColor Yellow
Write-Host ""
Write-Host "🧪 To test the backend:" -ForegroundColor Cyan
Write-Host "   .\test-backend.ps1" -ForegroundColor Yellow
Write-Host ""
Write-Host "🌐 Web UI will be available at:" -ForegroundColor Cyan
Write-Host "   http://localhost:8080" -ForegroundColor Yellow
Write-Host ""
