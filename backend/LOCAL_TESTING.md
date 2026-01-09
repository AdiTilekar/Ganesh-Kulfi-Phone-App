# Local Testing Guide - Before Deployment
**Test your Ganesh Kulfi Backend locally before deploying to production**

---

## 🎯 Two Testing Options

### Option 1: Docker Compose (Recommended - Easiest)
Uses the provided `docker-compose.yml` - includes PostgreSQL automatically.

### Option 2: Manual Testing (Use existing local PostgreSQL)
If you already have PostgreSQL installed locally.

---

## ✅ Option 1: Docker Compose Testing (Recommended)

### Prerequisites
- Docker Desktop installed and running
- Backend built (JAR file exists)

### Step 1: Build the Application
```powershell
cd "e:\Ganesh Kulfi web\KulfiDelightAndroid\backend"
.\gradlew.bat clean shadowJar
```

### Step 2: Create .env File
```powershell
# Create .env file for local testing
@"
DB_URL=jdbc:postgresql://postgres:5432/ganeshkulfi_db
DB_USER=ganeshkulfi_user
DB_PASSWORD=ganeshkulfi_password
DB_POOL_SIZE=10
JWT_SECRET=local-test-secret-key-change-in-production-min-256-bits
JWT_ISSUER=ganeshkulfi
JWT_AUDIENCE=ganeshkulfi-app
APP_PORT=8080
UPLOADS_DIR=/app/uploads
"@ | Out-File -FilePath .env -Encoding UTF8
```

### Step 3: Start Services
```powershell
# Start PostgreSQL + Backend
docker-compose up -d

# View logs
docker-compose logs -f backend
```

### Step 4: Test Health Endpoint
```powershell
# Wait 10-15 seconds for startup, then test
Start-Sleep -Seconds 15
Invoke-RestMethod -Uri "http://localhost:8080/health"
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

### Step 5: Stop Services
```powershell
# When done testing
docker-compose down

# To also delete database data
docker-compose down -v
```

---

## 🔧 Option 2: Manual Testing (Local PostgreSQL)

### Prerequisites
- PostgreSQL installed and running
- Database and user created

### Step 1: Setup Database
```powershell
# Connect to PostgreSQL
psql -U postgres

# In psql console:
CREATE DATABASE ganeshkulfi_db;
CREATE USER ganeshkulfi_user WITH PASSWORD 'your_local_password';
GRANT ALL PRIVILEGES ON DATABASE ganeshkulfi_db TO ganeshkulfi_user;
\q
```

### Step 2: Set Environment Variables
```powershell
# Set environment variables for current session
$env:DB_URL="jdbc:postgresql://localhost:5432/ganeshkulfi_db"
$env:DB_USER="ganeshkulfi_user"
$env:DB_PASSWORD="your_local_password"
$env:DB_POOL_SIZE="10"
$env:JWT_SECRET="local-test-secret-key-change-in-production-min-256-bits"
$env:JWT_ISSUER="ganeshkulfi"
$env:JWT_AUDIENCE="ganeshkulfi-app"
$env:APP_PORT="8080"
$env:UPLOADS_DIR="./uploads"
```

### Step 3: Build Application
```powershell
cd "e:\Ganesh Kulfi web\KulfiDelightAndroid\backend"
.\gradlew.bat clean shadowJar
```

### Step 4: Run Application
```powershell
java -Xms256m -Xmx512m -jar build\libs\ganeshkulfi-backend-all.jar
```

Watch for:
```
✅ Database migrations running...
✅ Application started
✅ Server listening on port 8080
```

---

## 🧪 Testing All Endpoints

### 1. Health Check
```powershell
# Basic health
Invoke-RestMethod -Uri "http://localhost:8080/health"

# Detailed health
Invoke-RestMethod -Uri "http://localhost:8080/api/health"
```

### 2. User Registration
```powershell
$registerBody = @{
    username = "testuser"
    password = "Test@123"
    fullName = "Test User"
    phoneNumber = "+919876543210"
    role = "CUSTOMER"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" `
    -Method POST `
    -Body $registerBody `
    -ContentType "application/json"
```

Expected response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "username": "testuser",
  "role": "CUSTOMER"
}
```

### 3. User Login
```powershell
$loginBody = @{
    username = "testuser"
    password = "Test@123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
    -Method POST `
    -Body $loginBody `
    -ContentType "application/json"

# Save token for next requests
$token = $response.token
Write-Host "Token: $token"
```

### 4. Get Products (requires auth)
```powershell
$headers = @{
    "Authorization" = "Bearer $token"
}

Invoke-RestMethod -Uri "http://localhost:8080/api/products" `
    -Method GET `
    -Headers $headers
```

### 5. Get Product by ID
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/products/1" `
    -Method GET `
    -Headers $headers
```

### 6. Create Order
```powershell
$orderBody = @{
    items = @(
        @{
            productId = 1
            quantity = 2
        },
        @{
            productId = 2
            quantity = 1
        }
    )
    deliveryAddress = "123 Test Street, Mumbai"
    paymentMethod = "CASH"
} | ConvertTo-Json -Depth 3

Invoke-RestMethod -Uri "http://localhost:8080/api/orders" `
    -Method POST `
    -Body $orderBody `
    -ContentType "application/json" `
    -Headers $headers
```

### 7. Get My Orders
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/orders/my-orders" `
    -Method GET `
    -Headers $headers
```

### 8. Admin Endpoints (need ADMIN token)

#### Register Admin User
```powershell
$adminBody = @{
    username = "admin"
    password = "Admin@123"
    fullName = "Admin User"
    phoneNumber = "+919876543211"
    role = "ADMIN"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" `
    -Method POST `
    -Body $adminBody `
    -ContentType "application/json"
```

#### Login as Admin
```powershell
$adminLoginBody = @{
    username = "admin"
    password = "Admin@123"
} | ConvertTo-Json

$adminResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" `
    -Method POST `
    -Body $adminLoginBody `
    -ContentType "application/json"

$adminToken = $adminResponse.token
$adminHeaders = @{
    "Authorization" = "Bearer $adminToken"
}
```

#### Get All Orders (Admin)
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/orders" `
    -Method GET `
    -Headers $adminHeaders
```

#### Get Analytics
```powershell
# Daily orders
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/analytics/daily-orders" `
    -Method GET `
    -Headers $adminHeaders

# Daily sales
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/analytics/daily-sales" `
    -Method GET `
    -Headers $adminHeaders

# Pending orders
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/analytics/pending-orders" `
    -Method GET `
    -Headers $adminHeaders

# Low stock
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/analytics/low-stock?threshold=10" `
    -Method GET `
    -Headers $adminHeaders
```

---

## 🌐 Use API Test UI (Recommended)

The backend includes a built-in API test UI!

### Step 1: Start the Backend
```powershell
# Using Docker Compose
docker-compose up -d

# Or manually
java -jar build\libs\ganeshkulfi-backend-all.jar
```

### Step 2: Open Browser
Navigate to: **http://localhost:8080/**

You'll see a beautiful web interface with:
- ✅ Health check status
- ✅ All API endpoints listed
- ✅ Easy testing interface
- ✅ Real-time responses

### Step 3: Test All Features
The UI includes buttons for:
- Authentication (Register/Login)
- Products (List/Get/Search)
- Orders (Create/List/Status)
- Admin endpoints
- Analytics dashboard

---

## 📝 Complete Testing Checklist

### Authentication ✅
- [ ] Register new customer user
- [ ] Login with customer credentials
- [ ] Register new admin user
- [ ] Login with admin credentials
- [ ] Test invalid credentials (should fail)

### Products ✅
- [ ] Get all products
- [ ] Get product by ID
- [ ] Search products by name
- [ ] Filter by category
- [ ] Check product images load

### Orders ✅
- [ ] Create new order (customer)
- [ ] Get my orders (customer)
- [ ] View order details
- [ ] Check order status
- [ ] Poll for order updates

### Admin Features ✅
- [ ] View all orders
- [ ] Update order status
- [ ] View analytics (daily orders/sales)
- [ ] Check pending orders
- [ ] View low stock items
- [ ] Update product stock
- [ ] Activate/deactivate products
- [ ] Update product images

### System Health ✅
- [ ] Health endpoint responds
- [ ] Database connectivity verified
- [ ] Uploads directory exists
- [ ] Logs show no errors
- [ ] All migrations ran successfully

---

## 🐛 Common Issues & Solutions

### Issue 1: Database Connection Failed
```
Error: password authentication failed
```

**Solution**:
```powershell
# Check PostgreSQL is running
Get-Service postgresql*

# Verify credentials in psql
psql -U ganeshkulfi_user -d ganeshkulfi_db

# Or use Docker Compose (includes database)
docker-compose up -d
```

### Issue 2: Port Already in Use
```
Error: Address already in use: bind
```

**Solution**:
```powershell
# Find process using port 8080
netstat -ano | findstr :8080

# Kill the process (replace PID)
Stop-Process -Id <PID> -Force

# Or use different port
$env:APP_PORT="8081"
```

### Issue 3: JAR Not Found
```
Error: Unable to access jarfile
```

**Solution**:
```powershell
# Rebuild the JAR
.\gradlew.bat clean shadowJar

# Verify it exists
Test-Path "build\libs\ganeshkulfi-backend-all.jar"
```

### Issue 4: Uploads Directory Permission
```
Error: Permission denied: uploads
```

**Solution**:
```powershell
# Create uploads directory
New-Item -ItemType Directory -Force -Path "uploads"

# Or let app auto-create (already implemented)
```

### Issue 5: Migration Errors
```
Error: Flyway migration failed
```

**Solution**:
```powershell
# Drop and recreate database
psql -U postgres -c "DROP DATABASE IF EXISTS ganeshkulfi_db;"
psql -U postgres -c "CREATE DATABASE ganeshkulfi_db;"

# Restart app - migrations will run fresh
```

---

## 📊 Testing with Postman (Alternative)

### Import Collection
1. Open Postman
2. Import → Raw text
3. Paste the collection from `backend/api-test-ui.html`
4. Set environment variable `BASE_URL` = `http://localhost:8080`

### Test Workflow
1. Register user → Save token
2. Login → Update token in environment
3. Test authenticated endpoints
4. Register admin → Save admin token
5. Test admin endpoints

---

## 🚀 Ready for Deployment?

Once all tests pass:

### ✅ Pre-Deployment Checklist
- [ ] All endpoints tested and working
- [ ] Database migrations successful
- [ ] No errors in logs
- [ ] Health checks passing
- [ ] Authentication working
- [ ] Orders can be created
- [ ] Admin features functional
- [ ] Analytics endpoints working
- [ ] Product images uploading
- [ ] Code committed to Git

### Next Steps
```powershell
# 1. Commit your code
git add .
git commit -m "Backend tested and ready for deployment"
git push origin main

# 2. Follow deployment guide
# See: RAILWAY_DEPLOYMENT.md (recommended)
# Or: ALTERNATIVE_DEPLOYMENTS.md (other options)

# 3. Deploy to Railway/Render/Fly.io

# 4. Update Android app with production URL
```

---

## 🎉 Quick Test Script

Save this as `test-backend.ps1`:

```powershell
# Quick Backend Test Script
$baseUrl = "http://localhost:8080"

Write-Host "🧪 Testing Ganesh Kulfi Backend..." -ForegroundColor Cyan

# Test 1: Health Check
Write-Host "`n1️⃣ Testing health endpoint..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/health"
    Write-Host "✅ Health: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "❌ Health check failed!" -ForegroundColor Red
    exit 1
}

# Test 2: Register User
Write-Host "`n2️⃣ Registering test user..." -ForegroundColor Yellow
$registerBody = @{
    username = "test_$(Get-Random)"
    password = "Test@123"
    fullName = "Test User"
    phoneNumber = "+91$(Get-Random -Minimum 1000000000 -Maximum 9999999999)"
    role = "CUSTOMER"
} | ConvertTo-Json

try {
    $register = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method POST -Body $registerBody -ContentType "application/json"
    $token = $register.token
    Write-Host "✅ User registered: $($register.username)" -ForegroundColor Green
} catch {
    Write-Host "❌ Registration failed!" -ForegroundColor Red
    exit 1
}

# Test 3: Get Products
Write-Host "`n3️⃣ Fetching products..." -ForegroundColor Yellow
$headers = @{ "Authorization" = "Bearer $token" }
try {
    $products = Invoke-RestMethod -Uri "$baseUrl/api/products" -Headers $headers
    Write-Host "✅ Found $($products.Count) products" -ForegroundColor Green
} catch {
    Write-Host "❌ Products fetch failed!" -ForegroundColor Red
    exit 1
}

Write-Host "`n🎉 All tests passed! Backend is ready for deployment!" -ForegroundColor Green
Write-Host "`nNext: See RAILWAY_DEPLOYMENT.md for deployment steps" -ForegroundColor Cyan
```

Run it:
```powershell
.\test-backend.ps1
```

---

**🎯 Start with Docker Compose (easiest) or use the web UI at http://localhost:8080/**
