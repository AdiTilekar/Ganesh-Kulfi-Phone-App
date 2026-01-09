# Railway.app Deployment Guide - Ganesh Kulfi Backend
**Platform**: Railway.app (Free $5/month credits)  
**Best for**: Small to medium apps with PostgreSQL

---

## 📋 Prerequisites

1. GitHub account
2. Your backend code pushed to GitHub
3. Railway.app account (sign up free with GitHub)

---

## 🚀 Step-by-Step Deployment

### Step 1: Push Code to GitHub

```powershell
# Navigate to your project
cd "e:\Ganesh Kulfi web\KulfiDelightAndroid"

# Initialize git if not already done
git init
git add .
git commit -m "Day 14: Production ready backend"

# Push to GitHub
git remote add origin https://github.com/YOUR_USERNAME/KulfiDelightAndroid.git
git branch -M main
git push -u origin main
```

### Step 2: Sign Up for Railway

1. Go to: https://railway.app
2. Click "Login" → "Login with GitHub"
3. Authorize Railway to access your repositories

### Step 3: Create New Project

1. Click "New Project" button
2. Select "Deploy from GitHub repo"
3. Choose your `KulfiDelightAndroid` repository
4. Railway will detect it's a monorepo

### Step 4: Add PostgreSQL Database

1. In your project, click "+ New"
2. Select "Database" → "Add PostgreSQL"
3. Railway creates database and provides connection variables automatically
4. Note: Railway provides these variables:
   - `PGHOST`
   - `PGPORT`
   - `PGUSER`
   - `PGPASSWORD`
   - `PGDATABASE`
   - `DATABASE_URL` (full connection string)

### Step 5: Configure Backend Service

1. Click "+ New" → "GitHub Repo" → Select your repo
2. Configure build settings:
   - **Root Directory**: `backend`
   - **Build Command**: `./gradlew shadowJar`
   - **Start Command**: `java -Xms256m -Xmx512m -jar build/libs/ganeshkulfi-backend-all.jar`

### Step 6: Set Environment Variables

Click on your backend service → "Variables" → Add these:

```bash
# Database (use Railway's PostgreSQL variables)
DB_URL=postgresql://${{Postgres.PGUSER}}:${{Postgres.PGPASSWORD}}@${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DB_USER=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
DB_POOL_SIZE=10

# JWT Configuration (CHANGE THESE!)
JWT_SECRET=change-this-to-a-super-secure-random-string-min-256-bits
JWT_ISSUER=ganeshkulfi
JWT_AUDIENCE=ganeshkulfi-app

# Application
APP_PORT=8080
UPLOADS_DIR=/app/uploads
```

**⚠️ IMPORTANT**: Generate a secure JWT_SECRET:

```powershell
# Windows PowerShell - Generate random 256-bit secret
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 64 | % {[char]$_})
```

### Step 7: Deploy!

1. Railway automatically starts building
2. Watch logs in real-time
3. Once deployed, Railway provides a URL like: `https://ganeshkulfi-backend-production.up.railway.app`

### Step 8: Verify Deployment

```powershell
# Check health endpoint
curl https://your-app.up.railway.app/health
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

## 🔧 Railway Configuration Files

### Create Nixpacks Configuration (Optional)

Railway uses Nixpacks for builds. Create `nixpacks.toml` in backend folder:

```toml
[phases.setup]
nixPkgs = ["openjdk21"]

[phases.build]
cmds = ["./gradlew shadowJar"]

[start]
cmd = "java -Xms256m -Xmx512m -jar build/libs/ganeshkulfi-backend-all.jar"
```

### Railway Service Configuration (Optional)

Create `railway.json` in backend folder:

```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "NIXPACKS",
    "buildCommand": "./gradlew clean shadowJar"
  },
  "deploy": {
    "startCommand": "java -Xms256m -Xmx512m -jar build/libs/ganeshkulfi-backend-all.jar",
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 10
  }
}
```

---

## 📱 Update Android App

Once deployed, update your Android app's base URL:

```kotlin
// app/src/main/res/values/strings.xml
<string name="base_url">https://your-app.up.railway.app</string>

// Or in build.gradle
buildTypes {
    release {
        buildConfigField("String", "BASE_URL", "\"https://your-app.up.railway.app\"")
    }
}
```

---

## 🎯 Custom Domain (Optional)

### Add Custom Domain in Railway

1. Go to your service → "Settings" → "Domains"
2. Click "Add Domain"
3. Enter your domain: `api.ganeshkulfi.com`
4. Railway provides DNS records to add to your domain registrar:
   ```
   Type: CNAME
   Name: api
   Value: your-app.up.railway.app
   ```

### Update DNS at Your Registrar

1. Go to your domain registrar (GoDaddy, Namecheap, etc.)
2. Add the CNAME record Railway provided
3. Wait for DNS propagation (5-30 minutes)

### Update Android App

```kotlin
buildConfigField("String", "BASE_URL", "\"https://api.ganeshkulfi.com\"")
```

---

## 💰 Railway Free Tier Details

### What's Included
- **$5 free credits/month** (no credit card required)
- **Execution time**: ~500 hours/month with 512MB RAM
- **PostgreSQL**: Included in free tier
- **Bandwidth**: 100 GB/month
- **Build minutes**: Unlimited

### Credit Usage
- Each service uses credits based on:
  - **vCPU**: $0.000463/vCPU/minute
  - **RAM**: $0.000231/GB/minute
  - **Disk**: $0.25/GB/month

### Typical Monthly Usage (Your App)
- Backend (256-512MB RAM, always running):
  - ~$3-4/month
- PostgreSQL (shared, light usage):
  - ~$1-2/month
- **Total**: ~$4-5/month (within free tier!)

---

## 🔍 Monitoring & Logs

### View Logs
1. Go to your service in Railway dashboard
2. Click "Deployments"
3. Click on latest deployment
4. View real-time logs

### Metrics
Railway provides:
- CPU usage
- Memory usage
- Network traffic
- Request count

### Alerts
Set up alerts in Settings → Notifications

---

## 🐛 Troubleshooting

### Build Fails

**Issue**: Gradle build fails
```bash
./gradlew: Permission denied
```

**Solution**: Add to `railway.json`:
```json
{
  "build": {
    "builder": "NIXPACKS",
    "buildCommand": "chmod +x gradlew && ./gradlew clean shadowJar"
  }
}
```

### Database Connection Failed

**Issue**: Backend can't connect to PostgreSQL
```
Connection refused to postgres
```

**Solution**: Check environment variables:
```bash
# Ensure DB_URL uses Railway's internal hostname
DB_URL=postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
```

### Out of Memory

**Issue**: Java heap space error
```
java.lang.OutOfMemoryError: Java heap space
```

**Solution**: Adjust JVM memory in start command:
```bash
java -Xms128m -Xmx384m -jar build/libs/ganeshkulfi-backend-all.jar
```

### Upload Directory Not Writable

**Issue**: Can't upload product images
```
Permission denied: /app/uploads
```

**Solution**: Railway volumes are persistent. Create volume:
1. Service → Settings → Volumes
2. Add volume: `/app/uploads`

---

## 🔄 Continuous Deployment

Railway automatically redeploys when you push to GitHub:

```powershell
# Make changes to your code
git add .
git commit -m "Updated product API"
git push origin main

# Railway automatically:
# 1. Detects push
# 2. Builds new version
# 3. Runs migrations
# 4. Deploys with zero downtime
```

---

## 📊 Railway Dashboard Overview

```
┌─────────────────────────────────────────┐
│ Project: Ganesh Kulfi Backend           │
├─────────────────────────────────────────┤
│ Services:                               │
│  ✅ Backend (ganeshkulfi-backend)      │
│     URL: https://your-app.up.railway.app│
│     Status: Deployed                    │
│     CPU: 0.5 vCPU                       │
│     RAM: 256 MB                         │
│                                         │
│  ✅ Postgres                           │
│     Status: Running                     │
│     Storage: 1 GB                       │
│                                         │
│ Monthly Usage: $4.23 / $5.00           │
│ Credits Remaining: $0.77                │
└─────────────────────────────────────────┘
```

---

## ✅ Deployment Checklist

Before going live, verify:

- [ ] Code pushed to GitHub
- [ ] Railway project created
- [ ] PostgreSQL database added
- [ ] Environment variables configured
- [ ] Strong JWT_SECRET set
- [ ] Backend deployed successfully
- [ ] Health endpoint responds
- [ ] Database migrations ran
- [ ] Android app updated with production URL
- [ ] Test user registration/login
- [ ] Test product catalog
- [ ] Test order creation
- [ ] Upload directory working

---

## 🎉 Next Steps

1. **Test thoroughly**: Try all API endpoints
2. **Monitor usage**: Check Railway dashboard daily
3. **Set up alerts**: Get notified of issues
4. **Add custom domain**: Professional URL
5. **Configure backups**: PostgreSQL automated backups
6. **Scale if needed**: Upgrade Railway plan when you outgrow free tier

---

## 💡 Tips for Staying in Free Tier

1. **Optimize memory**: Use -Xmx384m instead of 512m if possible
2. **Efficient queries**: Use database indexes, avoid N+1 queries
3. **Cache frequently accessed data**: Reduce database load
4. **Compress uploads**: Smaller image sizes = less storage
5. **Monitor usage**: Railway dashboard shows daily credit consumption

---

## 🆘 Need Help?

- **Railway Docs**: https://docs.railway.app
- **Railway Discord**: https://discord.gg/railway
- **Your Backend Docs**: See `DEPLOYMENT.md` for troubleshooting

---

**Ready to deploy? Follow the steps above and your backend will be live in ~10 minutes!** 🚀
