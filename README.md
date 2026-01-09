# 🍨 Ganesh Kulfi Management System

A full-stack inventory and order management system for **Shri Ganesh Kulfi** - a kulfi ice cream business in Kopargaon, Maharashtra. Built with **Jetpack Compose** (Android) and **Ktor** (Backend).

## ✨ Overview

Professional business management system supporting three user roles (Admin, Retailer, Factory) with complete inventory tracking, order management, and role-based access control. Features 13 authentic kulfi flavors with real-time stock management and secure JWT authentication.

### Key Features

- 🔐 **Secure Authentication**: JWT-based auth with BCrypt password hashing (work factor 12)
- 👥 **Multi-Role System**: Admin, Retailer, Factory with role-based access control
- 🍦 **13 Kulfi Flavors**: Malai, Mango, Kesar-Pista, Chocolate, Strawberry, Butterscotch, Kesar, Sitafal, Anjeer, Pista, Gulab-Jamun, Rajbhog, Kulfi
- 📦 **Inventory Management**: Real-time stock tracking with editable quantities
- 📱 **Order Processing**: Complete order lifecycle from creation to completion
- 🔍 **Advanced Search**: Filter by flavor, status, retailer with instant results
- 🎨 **Modern UI**: Material Design 3 with Jetpack Compose
- 🌍 **Multi-language**: English, Hindi, Marathi support
- 🔒 **Production Security**: B+ security rating, 23/27 issues resolved (85%)

## 🏗️ Architecture

**3-Tier Architecture:**
```
Android App (Kotlin + Jetpack Compose)
           ↓ Retrofit + OkHttp
Ktor REST API (Kotlin)
           ↓ Exposed ORM
PostgreSQL Database (18.1)
```

### Design Patterns
- **Frontend**: MVVM + Repository pattern, Unidirectional Data Flow
- **Backend**: Layered architecture (Routes → Services → Repositories → Database)
- **Dependency Injection**: Hilt (Android), Koin (Ktor)

## 🛠️ Tech Stack

### Frontend (Android)
- **Language**: Kotlin 1.9
- **UI**: Jetpack Compose (Material Design 3)
- **Navigation**: Compose Navigation
- **DI**: Hilt 2.48
- **Networking**: Retrofit 2.9, OkHttp 4.11 (30s timeouts)
- **Local DB**: Room 2.6.1
- **Image Loading**: Coil 2.5
- **Async**: Kotlin Coroutines + Flow

### Backend (Ktor)
- **Framework**: Ktor 2.3.7
- **Database**: PostgreSQL 18.1
- **ORM**: Exposed 0.45.0
- **Migration**: Flyway 9.22.3
- **Connection Pool**: HikariCP 5.1.0
- **Authentication**: JWT (30-day expiry)
- **Password**: BCrypt (work factor 12)
- **Serialization**: kotlinx.serialization
- **Logging**: Logback + SLF4J

### Deployment
- **Production**: Render.com (https://ganesh-kulfi-backend.onrender.com)
- **Database**: Render PostgreSQL 18.1
- **Build**: Gradle 8.x
- **Version**: 1.0.0 (code 10000)

## 🚀 Getting Started

### Prerequisites

**For Android:**
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Kotlin 1.9+

**For Backend:**
- JDK 17
- PostgreSQL 18.1 (or Render managed instance)
- Gradle 8.x

### Quick Setup

#### 1. Clone Repository
```powershell
git clone https://github.com/yourusername/ganesh-kulfi-system.git
cd ganesh-kulfi-system
```

#### 2. Backend Setup

**Option A: Use Deployed Backend (Recommended)**
```
No setup needed! The app is already configured to use:
https://ganesh-kulfi-backend.onrender.com
```

**Option B: Run Locally**

1. Install PostgreSQL 18.1
2. Create database:
```powershell
psql -U postgres
CREATE DATABASE ganesh_kulfi;
```

3. Configure environment:
```powershell
# Create backend/.env or set environment variables
DATABASE_URL=jdbc:postgresql://localhost:5432/ganesh_kulfi
DATABASE_USER=postgres
DATABASE_PASSWORD=your_password
JWT_SECRET=your_secret_key
```

4. Run backend:
```powershell
cd backend
.\gradlew.bat run
# Server starts at http://localhost:8080
```

5. Database migrations run automatically via Flyway

#### 3. Android App Setup

1. Open project in Android Studio
2. Wait for Gradle sync
3. Select build variant:
   - **debug**: Uses local backend (http://10.0.2.2:8080)
   - **release**: Uses production backend (https://ganesh-kulfi-backend.onrender.com)

4. Build APK:
```powershell
.\gradlew.bat assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Or use provided script:
```powershell
.\build.ps1
```

## 📁 Project Structure

```
ganesh-kulfi-system/
├── app/                          # Android application
│   ├── src/main/kotlin/com/ganeshkulfi/retailer/
│   │   ├── data/                 # Repositories, data sources
│   │   ├── domain/               # Models, use cases
│   │   ├── presentation/         # UI, ViewModels, Navigation
│   │   ├── di/                   # Hilt modules
│   │   └── utils/                # Utilities
│   └── build.gradle.kts          # Android build config
│
├── backend/                      # Ktor backend
│   ├── src/main/kotlin/com/ganeshkulfi/backend/
│   │   ├── routes/               # API endpoints
│   │   ├── services/             # Business logic
│   │   ├── repositories/         # Data access layer
│   │   ├── models/               # Data models
│   │   ├── plugins/              # Ktor plugins (Auth, Routing, etc.)
│   │   └── Application.kt        # Entry point
│   ├── scripts/                  # Utility scripts
│   │   ├── deploy.ps1           # Deployment script
│   │   ├── quick-start.ps1      # Quick local start
│   │   └── test-backend.ps1     # API testing
│   ├── sql/                      # SQL migrations
│   │   └── setup-database.sql   # Initial schema
│   ├── utils/                    # Utilities
│   │   └── api-test-ui.html     # API testing UI
│   └── build.gradle.kts         # Backend build config
│
├── docs/                         # Documentation
│   ├── HIGH_PRIORITY_FIXES_APPLIED.md
│   ├── MEDIUM_PRIORITY_FIXES_COMPLETE.md
│   ├── SECURITY_FIXES_APPLIED.md
│   ├── RENDER_DEPLOYMENT_GUIDE.md
│   └── RETAILER_PRICING_GUIDE.md
│
├── build.ps1                     # Android build script
└── README.md                     # This file
```

## 🔐 Security Features

**Achieved B+ Security Rating (85% - 23/27 issues resolved)**

### Implemented Protections

✅ **Authentication & Authorization**
- JWT tokens with 30-day expiry
- BCrypt password hashing (work factor 12)
- Role-based access control (Admin, Retailer, Factory)
- Secure password validation (min 6 chars)

✅ **Network Security**
- HTTPS in production (Render.com)
- Extended timeouts (30s) for slow networks
- Request/response validation
- Error message sanitization

✅ **Input Validation**
- SQL injection prevention (Exposed ORM)
- XSS protection
- Input sanitization on all endpoints
- File upload validation

✅ **Code Quality**
- Secrets in environment variables
- ProGuard/R8 obfuscation ready
- Secure data storage (Room encrypted)
- Dependency management

### Pending Improvements (Low Priority)
- Dependency vulnerability scanning
- Encrypted SharedPreferences
- Comprehensive unit tests
- CI/CD pipeline

## 🎯 User Roles & Permissions

### 1. Admin
**Full System Access**
- Manage all flavors (add, edit, delete)
- View all orders (any retailer)
- Manage users (create retailers, factory accounts)
- Inventory control (update stock levels)
- System statistics and reports
- Complete order lifecycle management

**Default Credentials:**
```
Username: admin@ganeshkulfi.com
Password: admin123
```

### 2. Retailer
**Order & Inventory Access**
- Browse all flavors
- Place orders (any quantity)
- View own orders only
- Track order status
- Update profile

**Test Retailer:**
```
Username: retailer@example.com
Password: retailer123
```

### 3. Factory
**Production Management**
- View all orders
- Update order status
- Mark orders as completed
- View inventory levels
- Cannot place orders

**Test Factory:**
```
Username: factory@example.com
Password: factory123
```

## 📱 App Features

### For All Users
- 🔍 **Search & Filter**: Instant search by flavor name, status, retailer
- 🎨 **Beautiful UI**: Material Design 3 with custom theme (Saffron/Cream)
- 🌍 **Multi-language**: Automatic language selection (EN/HI/MR)
- 🔔 **Real-time Updates**: Orders sync automatically
- 📴 **Offline Support**: Room caching for uninterrupted access

### Admin Features
- 📊 **Dashboard**: Business overview with statistics
- 🍦 **Flavor Management**: Add/edit flavors with validation
- 📦 **Inventory Control**: Bulk stock updates
- 👥 **User Management**: Create and manage accounts
- 📈 **Reports**: Order trends and analytics

### Retailer Features
- 🛒 **Order Placement**: Easy ordering with quantity selection
- 📋 **Order History**: Complete order tracking
- 💰 **Pricing Info**: View pricing tiers
- 📞 **Contact Admin**: Direct support access

### Factory Features
- 🏭 **Production Queue**: View pending orders
- ✅ **Order Completion**: Mark orders as fulfilled
- 📊 **Production Stats**: Daily/weekly reports

## 🌐 API Endpoints

**Base URL (Production):** `https://ganesh-kulfi-backend.onrender.com`

### Authentication
```http
POST /api/auth/login
POST /api/auth/register
POST /api/auth/refresh
```

### Flavors
```http
GET    /api/flavors              # Get all flavors
GET    /api/flavors/{id}         # Get by ID
POST   /api/flavors              # Create (Admin only)
PUT    /api/flavors/{id}         # Update (Admin only)
DELETE /api/flavors/{id}         # Delete (Admin only)
```

### Orders
```http
GET    /api/orders               # Get orders (role-filtered)
GET    /api/orders/{id}          # Get by ID
POST   /api/orders               # Create order
PUT    /api/orders/{id}          # Update order
PUT    /api/orders/{id}/status   # Update status
DELETE /api/orders/{id}          # Cancel order
```

### Users
```http
GET    /api/users                # Get all users (Admin only)
GET    /api/users/me             # Get current user
PUT    /api/users/{id}           # Update user (Admin only)
DELETE /api/users/{id}           # Delete user (Admin only)
```

**Authentication:** All protected endpoints require `Authorization: Bearer <JWT_TOKEN>` header.

## 🧪 Testing

### Backend API Testing

**Option 1: PowerShell Script**
```powershell
cd backend
.\scripts\test-backend.ps1
```

**Option 2: HTML Test UI**
```powershell
# Open backend/utils/api-test-ui.html in browser
# Enter base URL and test endpoints interactively
```

**Option 3: Manual Testing**
```powershell
# Login
curl -X POST https://ganesh-kulfi-backend.onrender.com/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"admin@ganeshkulfi.com","password":"admin123"}'

# Get flavors
curl https://ganesh-kulfi-backend.onrender.com/api/flavors `
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Android Testing

**Unit Tests:**
```powershell
.\gradlew.bat test
```

**Instrumentation Tests:**
```powershell
.\gradlew.bat connectedAndroidTest
```

## 🚀 Deployment

### Backend (Render.com)

**Current Production:** https://ganesh-kulfi-backend.onrender.com

**Manual Deploy:**
```powershell
cd backend
.\scripts\deploy.ps1
```

**Automatic Deploy:**
- Push to `main` branch
- Render detects changes and deploys automatically

**Environment Variables (Render):**
```env
DATABASE_URL=<Render PostgreSQL URL>
JWT_SECRET=<your-secret>
PORT=8080
```

### Android (APK Distribution)

**Debug Build:**
```powershell
.\build.ps1
# APK: app/build/outputs/apk/debug/app-debug.apk
```

**Release Build:**
```powershell
.\gradlew.bat assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

**Install on Device:**
```powershell
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🐛 Troubleshooting

### Backend Issues

**Problem: Database connection failed**
```
Solution: Check DATABASE_URL environment variable
Verify PostgreSQL is running (local) or Render service is active
```

**Problem: Port already in use**
```powershell
# Find process on port 8080
netstat -ano | findstr :8080
# Kill process
taskkill /PID <process_id> /F
```

**Problem: Migration failed**
```
Solution: Drop and recreate database
Run: psql -U postgres -c "DROP DATABASE ganesh_kulfi; CREATE DATABASE ganesh_kulfi;"
```

### Android Issues

**Problem: Build failed - SDK not found**
```
Solution: Set ANDROID_HOME environment variable
Or create local.properties with sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

**Problem: App crashes on startup**
```
Solution: Check backend URL in build.gradle.kts buildConfigField
Verify backend is running and accessible
Check Logcat for detailed error messages
```

**Problem: Login fails**
```
Solution: Verify backend is running
Check network permissions in AndroidManifest.xml
Use debug build for local backend (10.0.2.2:8080)
```

## 📊 Version Management

**Current Version:** 1.0.0 (code 10000)

Version format: `MAJOR.MINOR.PATCH`
- **MAJOR**: Breaking API changes
- **MINOR**: New features, backwards compatible
- **PATCH**: Bug fixes

Version code calculation: `MAJOR * 10000 + MINOR * 100 + PATCH`

## 🎤 Interview Highlights

### Technical Achievements
1. **Full-Stack Development**: Built complete Android app + Ktor backend + PostgreSQL database
2. **Security**: Achieved B+ rating with JWT auth, BCrypt hashing, RBAC
3. **Modern Architecture**: MVVM + Clean Architecture, 3-tier separation
4. **Production Ready**: Deployed on Render.com with automated migrations
5. **Code Quality**: Organized 30+ files, deleted 75MB unused code, 85% security compliance

### Key Technologies
- **Android**: Jetpack Compose, Hilt, Retrofit, Room, Coroutines
- **Backend**: Ktor, Exposed ORM, Flyway, HikariCP, JWT
- **Database**: PostgreSQL 18.1 with Flyway migrations
- **DevOps**: Gradle, Git, Render.com, PowerShell automation

### Problem-Solving Examples
1. **Network Timeouts**: Increased OkHttp timeouts from 10s to 30s for slow connections
2. **Order Number Conflicts**: Implemented database sequence for unique order numbers
3. **Password Security**: Migrated from plain text to BCrypt with work factor 12
4. **Deployment Issues**: Fixed Application.kt receiver type for Render compatibility
5. **Project Organization**: Restructured 30 files into logical folders (docs, scripts, sql, utils)

### Business Impact
- **3 User Roles**: Supports admin, retailer, factory workflows
- **13 Flavors**: Complete kulfi inventory management
- **Real-time Orders**: Instant order placement and tracking
- **Multi-language**: Serves English, Hindi, Marathi speakers
- **Scalable**: Production-ready architecture on Render.com

## 📄 License

This project is proprietary software for **Shri Ganesh Kulfi**.

## 👥 Credits

- **Business Owner**: Ganesh Raut
- **Location**: Kopargaon, Maharashtra, India
- **Developer**: Aditya Tilekar
- **Tech Stack**: Kotlin, Jetpack Compose, Ktor, PostgreSQL

## 📞 Contact

For questions or support:
- **Business**: Shri Ganesh Kulfi, Kopargaon
- **Technical**: Open an issue on GitHub

---

**Made with ❤️ for authentic kulfi lovers in Kopargaon**

*Version 1.0.0 - Production Ready - January 2026*
