#!/bin/bash
# Quick Deployment Script for Ganesh Kulfi Backend
# Version: 0.0.14-SNAPSHOT (Day 14: Production Ready)

set -e  # Exit on error

echo "🚀 Ganesh Kulfi Backend - Production Deployment"
echo "================================================"
echo ""

# Check if .env exists
if [ ! -f ".env" ]; then
    echo "⚠️  .env file not found!"
    echo "📝 Creating .env from template..."
    cp .env.example .env
    echo ""
    echo "✅ Created .env file"
    echo "⚠️  IMPORTANT: Edit .env with your production values before continuing!"
    echo ""
    echo "Run: nano .env"
    echo ""
    exit 1
fi

echo "✅ .env file found"
echo ""

# Check Docker installation
if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose not found. Please install Docker Compose first."
    exit 1
fi

echo "✅ Docker and Docker Compose found"
echo ""

# Build the application
echo "📦 Building application..."
./gradlew clean shadowJar

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✅ Build successful"
echo ""

# Start Docker services
echo "🐳 Starting Docker services..."
docker-compose up -d

if [ $? -ne 0 ]; then
    echo "❌ Docker Compose failed!"
    exit 1
fi

echo "✅ Docker services started"
echo ""

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check health endpoint
echo "🏥 Checking health endpoint..."
HEALTH_RESPONSE=$(curl -s http://localhost:8080/health || echo "")

if [ -z "$HEALTH_RESPONSE" ]; then
    echo "⚠️  Health check failed - service may still be starting up"
    echo "   View logs: docker-compose logs -f backend"
else
    echo "✅ Health check passed"
    echo ""
    echo "Response:"
    echo "$HEALTH_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$HEALTH_RESPONSE"
fi

echo ""
echo "================================================"
echo "✅ Deployment Complete!"
echo "================================================"
echo ""
echo "📊 Service Information:"
echo "  Backend URL: http://localhost:8080"
echo "  Health Check: http://localhost:8080/health"
echo "  API Health: http://localhost:8080/api/health"
echo ""
echo "📝 Useful Commands:"
echo "  View logs: docker-compose logs -f backend"
echo "  Stop services: docker-compose down"
echo "  Restart: docker-compose restart backend"
echo "  Check status: docker-compose ps"
echo ""
echo "📖 Documentation:"
echo "  Deployment Guide: DEPLOYMENT.md"
echo "  API Reference: README.md"
echo "  Day 14 Summary: DAY14_COMPLETE.md"
echo ""
