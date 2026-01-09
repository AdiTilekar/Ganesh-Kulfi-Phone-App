# Quick Start - Uses postgres/postgres as default credentials
# ⚠️  SECURITY WARNING: FOR LOCAL TESTING ONLY!
# ⚠️  NEVER use these credentials in production!
# ⚠️  Change password immediately for any deployment!

Write-Host "🚀 Quick Start - Ganesh Kulfi Backend" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  WARNING: Using default credentials for testing!" -ForegroundColor Red
Write-Host "⚠️  Database: postgres/postgres - CHANGE FOR PRODUCTION!" -ForegroundColor Red
Write-Host "" -ForegroundColor Red

# Default configuration for quick testing
$dbHost = "localhost"
$dbPort = "5432"
$dbName = "ganeshkulfi_db"
$dbUser = "postgres"
$dbPassword = "postgres"

Write-Host "📝 Using default PostgreSQL configuration:" -ForegroundColor Yellow
Write-Host "   Host: $dbHost" -ForegroundColor Gray
Write-Host "   Port: $dbPort" -ForegroundColor Gray
Write-Host "   Database: $dbName" -ForegroundColor Gray
Write-Host "   User: $dbUser" -ForegroundColor Gray
Write-Host ""
Write-Host "⚠️  Using 'postgres' as password. Change in setup-local.ps1 for custom config." -ForegroundColor Yellow
Write-Host ""

# Generate JWT secret
Write-Host "🔐 Generating JWT secret..." -ForegroundColor Yellow
$jwtSecret = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 64 | % {[char]$_})
Write-Host "✅ JWT secret generated" -ForegroundColor Green
Write-Host ""

# Create .env file
Write-Host "📝 Creating .env file..." -ForegroundColor Yellow
$envContent = @"
DB_URL=jdbc:postgresql://${dbHost}:${dbPort}/${dbName}
DB_USER=$dbUser
DB_PASSWORD=$dbPassword
DB_POOL_SIZE=10
JWT_SECRET=$jwtSecret
JWT_ISSUER=ganeshkulfi
JWT_AUDIENCE=ganeshkulfi-app
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
}
Write-Host "✅ Uploads directory ready" -ForegroundColor Green
Write-Host ""

# Set environment variables
Write-Host "🔧 Setting environment variables..." -ForegroundColor Yellow
$env:DB_URL = "jdbc:postgresql://${dbHost}:${dbPort}/${dbName}"
$env:DB_USER = $dbUser
$env:DB_PASSWORD = $dbPassword
$env:DB_POOL_SIZE = "10"
$env:JWT_SECRET = $jwtSecret
$env:JWT_ISSUER = "ganeshkulfi"
$env:JWT_AUDIENCE = "ganeshkulfi-app"
$env:APP_PORT = "8080"
$env:UPLOADS_DIR = "./uploads"
Write-Host "✅ Environment variables set" -ForegroundColor Green
Write-Host ""

# Check/Build JAR
Write-Host "📦 Checking application build..." -ForegroundColor Yellow
if (-not (Test-Path "build\libs\ganeshkulfi-backend-all.jar")) {
    Write-Host "   Building application..." -ForegroundColor Yellow
    .\gradlew.bat shadowJar --console=plain
    Write-Host ""
}
Write-Host "✅ Application ready" -ForegroundColor Green
Write-Host ""

# Try to create database (if doesn't exist)
Write-Host "🗄️  Checking database..." -ForegroundColor Yellow
try {
    $createDb = @"
SELECT 'CREATE DATABASE $dbName'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$dbName')\gexec
"@
    $createDb | psql -U $dbUser -h $dbHost -p $dbPort -d postgres 2>$null
    Write-Host "✅ Database ready" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Could not check database (will be created on first run)" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "✅ Setup Complete!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "🚀 To start the backend:" -ForegroundColor Cyan
Write-Host "   .\start-backend.ps1" -ForegroundColor Yellow
Write-Host ""
Write-Host "Or run directly:" -ForegroundColor Cyan
Write-Host "   java -jar build\libs\ganeshkulfi-backend-all.jar" -ForegroundColor Yellow
Write-Host ""
Write-Host "🧪 After starting, test with:" -ForegroundColor Cyan
Write-Host "   .\test-backend.ps1" -ForegroundColor Yellow
Write-Host ""
Write-Host "🌐 Web UI: http://localhost:8080" -ForegroundColor Yellow
Write-Host ""
