# Quick Deployment Script for Ganesh Kulfi Backend (Windows)
# Version: 0.0.14-SNAPSHOT (Day 14: Production Ready)

Write-Host "🚀 Ganesh Kulfi Backend - Production Deployment" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Check if .env exists
if (-not (Test-Path ".env")) {
    Write-Host "⚠️  .env file not found!" -ForegroundColor Yellow
    Write-Host "📝 Creating .env from template..." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env"
    Write-Host ""
    Write-Host "✅ Created .env file" -ForegroundColor Green
    Write-Host "⚠️  IMPORTANT: Edit .env with your production values before continuing!" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Run: notepad .env" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

Write-Host "✅ .env file found" -ForegroundColor Green
Write-Host ""

# Check Docker installation
try {
    $dockerVersion = docker --version
    $composeVersion = docker-compose --version
    Write-Host "✅ Docker and Docker Compose found" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "❌ Docker or Docker Compose not found. Please install Docker Desktop first." -ForegroundColor Red
    exit 1
}

# Build the application
Write-Host "📦 Building application..." -ForegroundColor Cyan
.\gradlew.bat clean shadowJar --console=plain

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Build successful" -ForegroundColor Green
Write-Host ""

# Start Docker services
Write-Host "🐳 Starting Docker services..." -ForegroundColor Cyan
docker-compose up -d

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Docker Compose failed!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Docker services started" -ForegroundColor Green
Write-Host ""

# Wait for services to be ready
Write-Host "⏳ Waiting for services to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Check health endpoint
Write-Host "🏥 Checking health endpoint..." -ForegroundColor Cyan
try {
    $healthResponse = Invoke-RestMethod -Uri "http://localhost:8080/health" -Method Get -TimeoutSec 5
    Write-Host "✅ Health check passed" -ForegroundColor Green
    Write-Host ""
    Write-Host "Response:" -ForegroundColor Cyan
    $healthResponse | ConvertTo-Json
} catch {
    Write-Host "⚠️  Health check failed - service may still be starting up" -ForegroundColor Yellow
    Write-Host "   View logs: docker-compose logs -f backend" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "✅ Deployment Complete!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📊 Service Information:" -ForegroundColor Cyan
Write-Host "  Backend URL: http://localhost:8080"
Write-Host "  Health Check: http://localhost:8080/health"
Write-Host "  API Health: http://localhost:8080/api/health"
Write-Host ""
Write-Host "📝 Useful Commands:" -ForegroundColor Cyan
Write-Host "  View logs: docker-compose logs -f backend"
Write-Host "  Stop services: docker-compose down"
Write-Host "  Restart: docker-compose restart backend"
Write-Host "  Check status: docker-compose ps"
Write-Host ""
Write-Host "📖 Documentation:" -ForegroundColor Cyan
Write-Host "  Deployment Guide: DEPLOYMENT.md"
Write-Host "  API Reference: README.md"
Write-Host "  Day 14 Summary: DAY14_COMPLETE.md"
Write-Host ""
