# Production Deployment Guide - Ganesh Kulfi Backend
**Version: 0.0.14-SNAPSHOT (Day 14: Production Ready)**

## 📋 Overview
This guide covers deploying the Ganesh Kulfi backend to production using Docker or direct JAR deployment.

---

## 🔧 Prerequisites

### Required Software
- **Java 21** (or later)
- **PostgreSQL 18.1** (or compatible)
- **Docker & Docker Compose** (for containerized deployment)

### Database Setup
1. Install PostgreSQL 18.1+
2. Create database: `ganeshkulfi_db`
3. Create user with appropriate permissions
4. Run migrations automatically on first start (Flyway)

---

## 🚀 Deployment Options

### Option 1: Docker Deployment (Recommended)

#### 1.1 Create Environment File
```bash
# Copy example environment file
cp .env.example .env

# Edit .env with your production values
nano .env
```

Required environment variables:
```bash
# Database Configuration
DB_URL=jdbc:postgresql://postgres:5432/ganeshkulfi_db
DB_USER=your_postgres_user
DB_PASSWORD=your_secure_password
DB_POOL_SIZE=10

# JWT Configuration
JWT_SECRET=your-super-secure-jwt-secret-min-256-bits
JWT_ISSUER=ganeshkulfi
JWT_AUDIENCE=ganeshkulfi-app

# Application Configuration
APP_PORT=8080
UPLOADS_DIR=/app/uploads
```

#### 1.2 Build and Start Services
```bash
# Build the application
./gradlew shadowJar

# Start all services (PostgreSQL + Backend)
docker-compose up -d

# View logs
docker-compose logs -f backend

# Stop services
docker-compose down

# Stop and remove volumes (WARNING: deletes database data)
docker-compose down -v
```

#### 1.3 Health Check
```bash
curl http://localhost:8080/health
```

Expected response:
```json
{
  "status": "healthy",
  "message": "Backend is running",
  "timestamp": 1234567890,
  "database": "connected"
}
```

---

### Option 2: Direct JAR Deployment

#### 2.1 Build Production JAR
```bash
# Clean and build
./gradlew clean shadowJar

# JAR location
build/libs/ganeshkulfi-backend-all.jar
```

#### 2.2 Set Environment Variables
```bash
# Linux/Mac
export DB_URL="jdbc:postgresql://localhost:5432/ganeshkulfi_db"
export DB_USER="your_db_user"
export DB_PASSWORD="your_db_password"
export DB_POOL_SIZE=10
export JWT_SECRET="your-jwt-secret"
export JWT_ISSUER="ganeshkulfi"
export JWT_AUDIENCE="ganeshkulfi-app"
export APP_PORT=8080
export UPLOADS_DIR="/var/www/ganeshkulfi/uploads"

# Windows PowerShell
$env:DB_URL="jdbc:postgresql://localhost:5432/ganeshkulfi_db"
$env:DB_USER="your_db_user"
$env:DB_PASSWORD="your_db_password"
$env:DB_POOL_SIZE="10"
$env:JWT_SECRET="your-jwt-secret"
$env:JWT_ISSUER="ganeshkulfi"
$env:JWT_AUDIENCE="ganeshkulfi-app"
$env:APP_PORT="8080"
$env:UPLOADS_DIR="C:\ganeshkulfi\uploads"
```

#### 2.3 Run Application
```bash
# Run with production settings
java -Xms256m -Xmx512m -jar build/libs/ganeshkulfi-backend-all.jar

# Run in background (Linux)
nohup java -Xms256m -Xmx512m -jar build/libs/ganeshkulfi-backend-all.jar > app.log 2>&1 &

# Run as systemd service (Linux)
sudo systemctl start ganeshkulfi-backend
```

---

## 🔐 Security Checklist

### Production Security
- [ ] Change default JWT_SECRET to a strong random value (min 256 bits)
- [ ] Use strong PostgreSQL password
- [ ] Enable HTTPS/SSL with reverse proxy (nginx/Apache)
- [ ] Configure firewall rules (allow only 8080 from localhost if using reverse proxy)
- [ ] Set up rate limiting
- [ ] Enable PostgreSQL SSL connections
- [ ] Regularly update dependencies (`./gradlew dependencyUpdates`)

### Database Security
```sql
-- Create dedicated database user
CREATE USER ganeshkulfi_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE ganeshkulfi_db TO ganeshkulfi_user;

-- Enable SSL (postgresql.conf)
ssl = on
ssl_cert_file = '/path/to/server.crt'
ssl_key_file = '/path/to/server.key'
```

---

## 📊 Production Monitoring

### Health Endpoints
```bash
# Application health
curl http://localhost:8080/health

# Detailed API health
curl http://localhost:8080/api/health
```

### Database Connection Pool Monitoring
Connection pool is configured with HikariCP:
- **Maximum Pool Size**: 10 connections
- **Connection Timeout**: 30 seconds
- **Idle Timeout**: 10 minutes
- **Max Lifetime**: 30 minutes

### Logs
```bash
# Docker logs
docker-compose logs -f backend

# Application logs (logback.xml)
# Located at: logs/application.log (if file appender is configured)
```

### Performance Metrics
Monitor these key indicators:
- Request latency (logged per request)
- Database connection pool usage
- Memory usage (JVM heap)
- Active connections
- Error rates

---

## 🔄 Maintenance

### Database Migrations
Migrations run automatically on startup using Flyway:
```
src/main/resources/db/migration/
├── V1__init.sql              # Initial schema
├── V2__products.sql          # Product data
├── V3__update_products...sql # Product updates
├── V4__inventory...sql       # Inventory system
└── ... (V5 through V11)      # Additional features
```

### Backup Strategy
```bash
# Backup PostgreSQL database
docker-compose exec postgres pg_dump -U ganeshkulfi_user ganeshkulfi_db > backup_$(date +%Y%m%d).sql

# Backup uploads directory
tar -czf uploads_backup_$(date +%Y%m%d).tar.gz /path/to/uploads

# Restore database
docker-compose exec -T postgres psql -U ganeshkulfi_user ganeshkulfi_db < backup_20240101.sql
```

### Updates and Rollbacks
```bash
# Deploy new version
git pull origin main
./gradlew clean shadowJar
docker-compose up -d --build

# Rollback (using Docker tags)
docker-compose down
docker-compose up -d ganeshkulfi-backend:v0.0.13
```

---

## 🌐 Reverse Proxy Setup (nginx)

### Example nginx Configuration
```nginx
upstream ganeshkulfi_backend {
    server localhost:8080;
}

server {
    listen 80;
    server_name api.ganeshkulfi.com;
    
    # Redirect to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.ganeshkulfi.com;
    
    # SSL certificates
    ssl_certificate /etc/letsencrypt/live/api.ganeshkulfi.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.ganeshkulfi.com/privkey.pem;
    
    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header X-Content-Type-Options "nosniff" always;
    
    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;
    limit_req zone=api_limit burst=20 nodelay;
    
    # Proxy settings
    location / {
        proxy_pass http://ganeshkulfi_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
    
    # Static file uploads
    location /uploads/ {
        alias /var/www/ganeshkulfi/uploads/;
        expires 7d;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## 📱 Android App Configuration

Update your Android app to point to production:
```kotlin
// app/src/main/res/values/strings.xml
<string name="base_url">https://api.ganeshkulfi.com</string>

// Or environment-specific configuration
buildTypes {
    release {
        buildConfigField("String", "BASE_URL", "\"https://api.ganeshkulfi.com\"")
    }
    debug {
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080\"")
    }
}
```

---

## 🐛 Troubleshooting

### Common Issues

#### 1. Database Connection Failed
```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Check database credentials
docker-compose exec postgres psql -U ganeshkulfi_user -d ganeshkulfi_db

# View database logs
docker-compose logs postgres
```

#### 2. Port Already in Use
```bash
# Find process using port 8080
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # Linux/Mac

# Change APP_PORT in .env
APP_PORT=8081
```

#### 3. Uploads Directory Permission Denied
```bash
# Fix permissions (Linux)
sudo chown -R $(whoami):$(whoami) /path/to/uploads
chmod 755 /path/to/uploads

# Docker volume permissions
docker-compose exec backend chown -R root:root /app/uploads
```

#### 4. Out of Memory
```bash
# Increase JVM heap size
java -Xms512m -Xmx1024m -jar ganeshkulfi-backend-all.jar

# Monitor memory usage
docker stats ganeshkulfi-backend
```

#### 5. Migration Errors
```bash
# Check Flyway schema history
docker-compose exec postgres psql -U ganeshkulfi_user -d ganeshkulfi_db -c "SELECT * FROM flyway_schema_history;"

# Reset database (WARNING: deletes all data)
docker-compose down -v
docker-compose up -d
```

---

## 📈 Production Optimization

### Database Tuning (postgresql.conf)
```conf
# Connection Pool
max_connections = 100
shared_buffers = 256MB
effective_cache_size = 1GB
maintenance_work_mem = 64MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200
work_mem = 4MB
min_wal_size = 1GB
max_wal_size = 4GB
```

### JVM Tuning
```bash
java \
  -Xms512m \
  -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -jar ganeshkulfi-backend-all.jar
```

---

## 📞 Support & Contact

- **Repository**: [GitHub Link]
- **Documentation**: See README.md for API endpoints
- **Issues**: Report bugs via GitHub Issues
- **Version**: 0.0.14-SNAPSHOT

---

## 🎯 Quick Start Commands

```bash
# Clone repository
git clone [repo-url]
cd KulfiDelightAndroid/backend

# Configure environment
cp .env.example .env
nano .env

# Build and deploy with Docker
./gradlew shadowJar
docker-compose up -d

# Check health
curl http://localhost:8080/health

# View logs
docker-compose logs -f backend
```

---

## ✅ Day 14 Production Features

- ✅ **Environment Variables**: All configuration externalized to .env
- ✅ **HikariCP Connection Pool**: Production-grade database pooling (max 10 connections)
- ✅ **Production Logging**: Request/response logging with CallLogging plugin
- ✅ **Health Checks**: `/health` and `/api/health` endpoints with database connectivity verification
- ✅ **Docker Support**: Complete docker-compose setup with PostgreSQL
- ✅ **Auto-creation**: Uploads directory created automatically on startup
- ✅ **Security**: JWT authentication, password hashing (BCrypt), CORS configuration
- ✅ **Performance**: Prepared statement caching, connection pooling, optimized transaction isolation
- ✅ **Maintainability**: Flyway migrations, structured logging, health monitoring

---

**Last Updated**: Day 14 - Production Deployment Complete
**Build Size**: 28.5 MB (ganeshkulfi-backend-all.jar)
**Database**: PostgreSQL 18.1
**Runtime**: Java 21
