# 🚀 Deployment Guide - Render.com + GitHub

## ✅ **Prerequisites Completed**
- ✅ Backend Dockerfile configured
- ✅ Render.yaml blueprint created
- ✅ Git repository initialized

---

## 📋 **Step-by-Step Deployment**

### **Step 1: Push Code to GitHub** (5 minutes)

1. **Commit all changes:**
```bash
git add .
git commit -m "Production ready - configured for Render.com deployment"
```

2. **Create GitHub repository:**
   - Go to https://github.com/new
   - Repository name: `ganesh-kulfi-app`
   - Make it **Private** (recommended)
   - Don't initialize with README (we already have one)
   - Click "Create repository"

3. **Push to GitHub:**
```bash
git remote add origin https://github.com/YOUR_USERNAME/ganesh-kulfi-app.git
git branch -M main
git push -u origin main
```

---

### **Step 2: Deploy Backend on Render.com** (10 minutes)

1. **Sign up/Login to Render:**
   - Go to https://render.com
   - Click "Sign up with GitHub"
   - Authorize Render to access your repositories

2. **Create New Web Service:**
   - Click "New +" → "Web Service"
   - Connect your `ganesh-kulfi-app` repository
   - Click "Connect"

3. **Configure Web Service:**
   ```
   Name:              ganesh-kulfi-backend
   Region:            Singapore (closest to India)
   Branch:            main
   Root Directory:    backend
   Runtime:           Docker
   Instance Type:     Free
   ```

4. **Click "Create Web Service"**
   - Render will automatically detect the Dockerfile
   - First deployment takes 5-10 minutes

---

### **Step 3: Create PostgreSQL Database** (5 minutes)

1. **Add Database:**
   - In Render dashboard, click "New +" → "PostgreSQL"
   - Configure:
     ```
     Name:         ganesh-kulfi-db
     Database:     ganeshkulfi
     User:         ganeshkulfi
     Region:       Singapore (same as backend)
     Plan:         Free
     ```
   - Click "Create Database"

2. **Connect Database to Backend:**
   - Go to your web service page
   - Click "Environment" tab
   - Add environment variables:
     ```
     DATABASE_URL     = [Copy from database "Internal Database URL"]
     JWT_SECRET       = ganeshkulfi_secret_key_2024_very_secure_production
     JWT_ISSUER       = ganeshkulfi
     JWT_AUDIENCE     = ganeshkulfi-app
     PORT             = 8080
     ```

3. **Manual Deploy:**
   - Click "Manual Deploy" → "Deploy latest commit"
   - Wait 3-5 minutes for deployment

---

### **Step 4: Initialize Database** (3 minutes)

Your backend URL will be: `https://ganesh-kulfi-backend.onrender.com`

**Render automatically runs Flyway migrations on startup!** ✅

To verify:
```bash
curl https://ganesh-kulfi-backend.onrender.com/api/health
```

Expected response:
```json
{"status":"healthy"}
```

---

### **Step 5: Update App BASE_URL** (2 minutes)

Update the app to use production backend:

**File:** `app/src/main/java/com/ganeshkulfi/app/data/remote/ApiService.kt`

```kotlin
object ApiService {
    // Production URL
    private const val BASE_URL = "https://ganesh-kulfi-backend.onrender.com"
    
    // For testing, you can switch between:
    // private const val BASE_URL = "http://10.242.116.68:8080"  // Local
    // private const val BASE_URL = "https://ganesh-kulfi-backend.onrender.com"  // Production
```

Then rebuild:
```bash
.\gradlew.bat assembleDebug
```

---

### **Step 6: Test Production Backend** (5 minutes)

```bash
# Test health check
curl https://ganesh-kulfi-backend.onrender.com/api/health

# Test login
curl -X POST https://ganesh-kulfi-backend.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@ganeshkulfi.com","password":"Admin1234"}'

# Test products
curl https://ganesh-kulfi-backend.onrender.com/api/products
```

---

## 🎉 **Deployment Complete!**

Your backend is now:
- ✅ Running on Render.com (free tier)
- ✅ PostgreSQL database (free tier)
- ✅ HTTPS enabled automatically
- ✅ Auto-deploy on git push
- ✅ Health checks enabled
- ✅ No monthly costs!

---

## 🔄 **Future Updates**

Every time you push to GitHub:
```bash
git add .
git commit -m "Update description"
git push
```

Render will **automatically**:
1. Detect the push
2. Build the Docker image
3. Run tests
4. Deploy new version
5. Zero downtime!

---

## 📊 **Render.com Free Tier Limits**

| Resource | Free Tier Limit |
|----------|----------------|
| **Web Service** | 750 hours/month (24/7) |
| **Database** | 1 GB storage |
| **Bandwidth** | 100 GB/month |
| **Builds** | Unlimited |
| **SSL/HTTPS** | ✅ Free |

**More than enough for your app!**

---

## 🚨 **Important Notes**

### **Free Tier Sleep Mode:**
- Backend sleeps after 15 minutes of inactivity
- Wakes up on first request (takes 30-50 seconds)
- To prevent: Upgrade to paid tier ($7/month) OR use a cron job to ping every 10 minutes

### **Database Persistence:**
- Free PostgreSQL is persistent (data won't be lost)
- Backed up automatically

### **Monitoring:**
- Dashboard shows logs, metrics, deployments
- Email alerts for deployment failures

---

## 🎯 **Next Steps**

1. ✅ Push code to GitHub
2. ✅ Deploy to Render.com
3. ✅ Update app BASE_URL
4. ✅ Test production backend
5. 📱 Build release APK
6. 🏪 Publish to Google Play Store

---

## 🆘 **Troubleshooting**

### **Deployment Failed:**
```bash
# Check logs in Render dashboard
# Common issues:
# - Build timeout: Increase build command timeout
# - Out of memory: Check Dockerfile memory settings
```

### **Database Connection Failed:**
```bash
# Verify DATABASE_URL is set correctly
# Check database is in same region
# Ensure Flyway migrations ran successfully
```

### **API Returns 502:**
```bash
# Backend is probably sleeping (free tier)
# Wait 30-50 seconds and retry
# Or upgrade to paid tier
```

---

## 📞 **Support**

- Render Docs: https://render.com/docs
- Community: https://community.render.com
- Status: https://status.render.com

---

**Backend URL:** https://ganesh-kulfi-backend.onrender.com  
**Admin Login:** admin@ganeshkulfi.com / Admin1234  
**Health Check:** https://ganesh-kulfi-backend.onrender.com/api/health

🎊 **Congratulations! Your backend is production-ready!**
