# Alternative Free Deployment Platforms

## 🎯 Quick Comparison

| Platform | Free Tier | PostgreSQL | Setup Time | Difficulty |
|----------|-----------|------------|------------|------------|
| **Railway** ⭐ | $5/mo credits | ✅ Free | 5 min | Easy |
| **Render** | 750 hrs/mo | ⚠️ 90 days | 10 min | Easy |
| **Fly.io** | 3 VMs | ✅ Free | 15 min | Medium |
| **Koyeb** | 512MB free | ❌ External | 10 min | Easy |

---

## 1️⃣ Render.com Deployment

### Pros
- Simple setup
- Auto-deploy from GitHub
- Free SSL/HTTPS
- 750 hours/month free

### Cons
- ⚠️ **Cold starts** (sleeps after 15 min inactivity)
- ⚠️ **PostgreSQL** free tier expires after 90 days
- First request takes 30-60 seconds to wake

### Quick Setup

1. **Sign up**: https://render.com (use GitHub)

2. **Create PostgreSQL**:
   - New → PostgreSQL
   - Name: `ganeshkulfi-db`
   - Database: `ganeshkulfi_db`
   - User: `ganeshkulfi_user`
   - Region: Choose closest to your users
   - Plan: Free
   - Note the "Internal Database URL"

3. **Create Web Service**:
   - New → Web Service
   - Connect GitHub repo
   - Root Directory: `backend`
   - Build Command: `./gradlew shadowJar`
   - Start Command: `java -Xms256m -Xmx512m -jar build/libs/ganeshkulfi-backend-all.jar`

4. **Environment Variables**:
   ```bash
   DB_URL=<Internal_Database_URL_from_step_2>
   DB_USER=ganeshkulfi_user
   DB_PASSWORD=<password_from_step_2>
   DB_POOL_SIZE=5
   JWT_SECRET=<generate_secure_random_string>
   JWT_ISSUER=ganeshkulfi
   JWT_AUDIENCE=ganeshkulfi-app
   APP_PORT=8080
   UPLOADS_DIR=/opt/render/project/src/uploads
   ```

5. **Deploy**: Render auto-builds and deploys

6. **URL**: `https://ganeshkulfi-backend.onrender.com`

### Dealing with Cold Starts

**Option A**: Keep-alive ping (from Android app):
```kotlin
// Ping every 10 minutes to keep awake
WorkManager.enqueuePeriodicWork(
    PeriodicWorkRequestBuilder<KeepAliveWorker>(10, TimeUnit.MINUTES).build()
)
```

**Option B**: External cron service:
- Use https://cron-job.org (free)
- Ping your health endpoint every 14 minutes

---

## 2️⃣ Fly.io Deployment

### Pros
- No cold starts
- Great free tier (3 VMs with 256MB each)
- Global CDN
- PostgreSQL included

### Cons
- CLI-based (terminal setup)
- Slightly steeper learning curve

### Quick Setup

1. **Install Fly CLI**:
   ```powershell
   # Windows PowerShell
   iwr https://fly.io/install.ps1 -useb | iex
   ```

2. **Login**:
   ```bash
   fly auth login
   ```

3. **Navigate to backend**:
   ```bash
   cd "e:\Ganesh Kulfi web\KulfiDelightAndroid\backend"
   ```

4. **Launch app**:
   ```bash
   fly launch
   ```
   - App name: `ganeshkulfi-backend`
   - Region: Choose closest to your users
   - PostgreSQL: Yes
   - Deploy now: No (we'll configure first)

5. **Configure secrets**:
   ```bash
   fly secrets set JWT_SECRET="your-super-secure-secret-here"
   fly secrets set JWT_ISSUER="ganeshkulfi"
   fly secrets set JWT_AUDIENCE="ganeshkulfi-app"
   ```

6. **Deploy**:
   ```bash
   fly deploy
   ```

7. **URL**: `https://ganeshkulfi-backend.fly.dev`

### Fly.toml Configuration

The `fly launch` command creates `fly.toml`. Customize it:

```toml
app = "ganeshkulfi-backend"
primary_region = "sin"  # Singapore, change as needed

[build]
  dockerfile = "Dockerfile"

[env]
  APP_PORT = "8080"
  UPLOADS_DIR = "/app/uploads"
  DB_POOL_SIZE = "5"

[[services]]
  internal_port = 8080
  protocol = "tcp"

  [[services.ports]]
    force_https = true
    handlers = ["http"]
    port = 80

  [[services.ports]]
    handlers = ["tls", "http"]
    port = 443

  [[services.http_checks]]
    interval = "30s"
    timeout = "5s"
    grace_period = "10s"
    method = "GET"
    path = "/health"

[[vm]]
  memory = "256mb"
  cpu_kind = "shared"
  cpus = 1
```

---

## 3️⃣ Koyeb Deployment

### Pros
- 512MB free instance
- No cold starts
- Simple web UI
- Auto-deploy from GitHub

### Cons
- No free PostgreSQL (use external like Supabase)
- Smaller free tier

### Quick Setup

1. **Get Free PostgreSQL from Supabase**:
   - Sign up: https://supabase.com (free)
   - Create project: `ganeshkulfi-db`
   - Go to Settings → Database
   - Copy connection string (direct connection, not pooler)
   - Format: `postgresql://postgres:[password]@[host]:5432/postgres`

2. **Deploy to Koyeb**:
   - Sign up: https://koyeb.com (use GitHub)
   - Create App → GitHub
   - Select your repo
   - Root path: `backend`
   - Build command: `./gradlew shadowJar`
   - Run command: `java -Xms128m -Xmx256m -jar build/libs/ganeshkulfi-backend-all.jar`

3. **Environment Variables**:
   ```bash
   DB_URL=<supabase_connection_string>
   DB_USER=postgres
   DB_PASSWORD=<supabase_password>
   DB_POOL_SIZE=3
   JWT_SECRET=<secure_random_string>
   JWT_ISSUER=ganeshkulfi
   JWT_AUDIENCE=ganeshkulfi-app
   APP_PORT=8080
   UPLOADS_DIR=/app/uploads
   ```

4. **Deploy**: Koyeb auto-deploys

5. **URL**: `https://ganeshkulfi-backend-[random].koyeb.app`

---

## 4️⃣ Supabase (PostgreSQL Only)

If you're deploying backend elsewhere but need free PostgreSQL:

### Setup
1. Sign up: https://supabase.com
2. Create project: `ganeshkulfi-db`
3. Settings → Database → Connection string
4. Use the **Direct connection** string (not pooler for Flyway migrations)

### Connection String Format
```
Direct: postgresql://postgres:[password]@db.[ref].supabase.co:5432/postgres
Pooler: postgresql://postgres:[password]@db.[ref].supabase.co:6543/postgres
```

**Use Direct connection** for your backend.

---

## 🏆 Recommendation by Use Case

### For Production (Real Users)
**Railway** or **Fly.io**
- No cold starts
- Reliable uptime
- Easy scaling

### For Development/Testing
**Render**
- Quick setup
- Free SSL
- Cold starts OK for testing

### For Learning/Portfolio
**Railway**
- Best free tier
- Easiest setup
- Great dashboard

---

## 💰 Cost Comparison (After Free Tier)

| Platform | Hobby Plan | Includes |
|----------|------------|----------|
| Railway | $5/mo (credits) | Everything you need |
| Render | $7/mo | Web service only |
| Fly.io | ~$5-10/mo | Pay as you go |
| Koyeb | €5/mo | 2GB RAM |

---

## 🚀 My Recommendation: **Railway**

For your Ganesh Kulfi app, I recommend **Railway** because:
1. ✅ Easiest setup (10 minutes)
2. ✅ PostgreSQL included
3. ✅ No cold starts
4. ✅ $5 free credits = perfect for your app size
5. ✅ Auto-deploy from GitHub
6. ✅ Great monitoring dashboard
7. ✅ Can scale easily when you grow

See `RAILWAY_DEPLOYMENT.md` for complete step-by-step guide!

---

## 📱 After Deployment

Update your Android app:

```kotlin
// app/build.gradle.kts
android {
    buildTypes {
        release {
            buildConfigField("String", "BASE_URL", "\"https://your-app.up.railway.app\"")
        }
        debug {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080\"")
        }
    }
}

// Use in code
val apiUrl = BuildConfig.BASE_URL
```

---

**Need help? Check the detailed guide for your chosen platform!**
