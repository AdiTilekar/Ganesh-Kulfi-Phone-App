# Environment Variables - Complete Reference

## 🔐 Required Environment Variables

All these variables MUST be set before running the backend in production.

### **Database Configuration**

```bash
# PostgreSQL connection URL (Render format: postgresql://user:pass@host:port/db)
DATABASE_URL=postgresql://username:password@hostname:5432/database_name

# Alternative format (legacy, used by some setups)
DB_URL=jdbc:postgresql://hostname:5432/database_name
DB_USER=username
DB_PASSWORD=password
DB_POOL_SIZE=10
```

**Production Example (Render.com):**
```bash
DATABASE_URL=postgresql://ganeshkulfi_user:SECURE_PASSWORD@dpg-xxxxx.oregon-postgres.render.com/ganeshkulfi_db
```

**Local Development:**
```bash
DB_URL=jdbc:postgresql://localhost:5432/ganeshkulfi_db
DB_USER=ganeshkulfi_user
DB_PASSWORD=kulfi@123  # ⚠️ Change for production!
DB_POOL_SIZE=10
```

---

### **JWT Configuration**

```bash
# JWT Secret - MUST be at least 64 characters, randomly generated
# Generate with: openssl rand -base64 64
JWT_SECRET=your_super_secure_random_64_character_minimum_secret_key_here_no_really_make_it_long

# JWT Issuer (your domain or app name)
JWT_ISSUER=ganeshkulfi

# JWT Audience (who can use the tokens)
JWT_AUDIENCE=ganeshkulfi-app
```

**⚠️ CRITICAL SECURITY:**
- Never commit JWT_SECRET to git
- Use different secrets for dev/staging/production
- Rotate secrets periodically
- Minimum 64 characters recommended

**Generate Strong Secret:**
```bash
# Linux/Mac:
openssl rand -base64 64

# PowerShell:
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 64 | % {[char]$_})
```

---

### **Application Configuration**

```bash
# Port the backend listens on (default: 8080)
APP_PORT=8080

# Directory for uploaded files (product images, etc.)
UPLOADS_DIR=./uploads
```

---

## 📋 Complete .env Template

Create a `.env` file in the `backend/` directory:

```bash
# ============================================
# Ganesh Kulfi Backend - Environment Variables
# ============================================
# Copy this template and fill in your values
# ⚠️ NEVER commit .env to git!
# ============================================

# Database Configuration (Choose ONE format)
# -----------------------------------------
# Format 1: Render.com / Heroku style
DATABASE_URL=postgresql://username:password@hostname:5432/database_name

# Format 2: Legacy JDBC style (for local development)
# DB_URL=jdbc:postgresql://localhost:5432/ganeshkulfi_db
# DB_USER=ganeshkulfi_user
# DB_PASSWORD=your_secure_password_here
# DB_POOL_SIZE=10

# JWT Configuration (ALL REQUIRED)
# -----------------------------------------
JWT_SECRET=GENERATE_64_CHAR_SECRET_HERE
JWT_ISSUER=ganeshkulfi
JWT_AUDIENCE=ganeshkulfi-app

# Application Configuration
# -----------------------------------------
APP_PORT=8080
UPLOADS_DIR=./uploads
```

---

## 🔍 How to Verify Configuration

### **Check if all variables are set:**

```bash
# Linux/Mac:
./backend/check-env.sh

# PowerShell:
.\backend\check-env.ps1
```

### **Manual verification:**

```bash
# Linux/Mac:
echo $JWT_SECRET
echo $DATABASE_URL

# PowerShell:
$env:JWT_SECRET
$env:DATABASE_URL
```

---

## 🚀 Setting Variables by Platform

### **1. Local Development**

Create `backend/.env` file (see template above).

**PowerShell:**
```powershell
$env:JWT_SECRET = "your_secret_here"
$env:DATABASE_URL = "postgresql://..."
```

**Linux/Mac:**
```bash
export JWT_SECRET="your_secret_here"
export DATABASE_URL="postgresql://..."
```

---

### **2. Render.com Deployment**

1. Go to your Render web service dashboard
2. Click **Environment** tab
3. Add each variable:

| Key | Value | Notes |
|-----|-------|-------|
| `DATABASE_URL` | (Auto from Render database) | Copy from database "Internal Database URL" |
| `JWT_SECRET` | (Generate 64+ chars) | Use `openssl rand -base64 64` |
| `JWT_ISSUER` | `ganeshkulfi` | Your app identifier |
| `JWT_AUDIENCE` | `ganeshkulfi-app` | Token audience |
| `APP_PORT` | `8080` | Must match Render port |
| `UPLOADS_DIR` | `/app/uploads` | Absolute path on Render |

**⚠️ Important:** After adding variables, Render will automatically redeploy.

---

### **3. Docker Deployment**

**docker-compose.yml:**
```yaml
services:
  backend:
    image: ganeshkulfi-backend
    environment:
      - DATABASE_URL=${DATABASE_URL}
      - JWT_SECRET=${JWT_SECRET}
      - JWT_ISSUER=${JWT_ISSUER}
      - JWT_AUDIENCE=${JWT_AUDIENCE}
      - APP_PORT=8080
      - UPLOADS_DIR=/app/uploads
    env_file:
      - .env  # Load from file
```

**Or pass directly:**
```bash
docker run -e JWT_SECRET="..." -e DATABASE_URL="..." ganeshkulfi-backend
```

---

### **4. Railway.app Deployment**

1. Go to your Railway project
2. Select your service
3. Click **Variables** tab
4. Add each environment variable
5. Click **Deploy**

---

## 🛡️ Security Best Practices

### **✅ DO:**
- ✅ Use `.env` file locally (add to `.gitignore`)
- ✅ Generate unique secrets for each environment
- ✅ Use strong, random passwords (64+ chars)
- ✅ Rotate secrets periodically (every 90 days)
- ✅ Use secret managers (AWS Secrets Manager, etc.) in production
- ✅ Limit access to environment variables
- ✅ Use HTTPS in production (secrets in transit)

### **❌ DON'T:**
- ❌ Commit `.env` file to git
- ❌ Share secrets in Slack/email
- ❌ Use weak passwords like "password123"
- ❌ Use same secrets across environments
- ❌ Hardcode secrets in source code
- ❌ Log secret values
- ❌ Use default/example secrets in production

---

## 🔧 Troubleshooting

### **"JWT_SECRET environment variable must be set"**

**Cause:** JWT_SECRET not configured.

**Fix:**
```bash
# Check if set:
echo $JWT_SECRET

# If empty, generate and set:
export JWT_SECRET=$(openssl rand -base64 64)
```

---

### **"Host is null in DATABASE_URL"**

**Cause:** DATABASE_URL format incorrect.

**Fix:** Ensure format is:
```
postgresql://username:password@hostname:port/database
```

---

### **Connection timeout / Database unreachable**

**Cause:** Database URL incorrect or database not running.

**Fix:**
1. Verify DATABASE_URL: `echo $DATABASE_URL`
2. Test connection: `psql $DATABASE_URL`
3. Check firewall/security groups
4. Verify database is running

---

### **Flyway migration errors**

**Cause:** Database schema mismatch or missing tables.

**Fix:**
```sql
-- Check migration history:
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC;

-- If needed, repair:
-- (Manually fix failed migration, then update flyway_schema_history)
```

---

## 📚 Reference Scripts

All setup scripts use these environment variables:

| Script | Variables Used |
|--------|---------------|
| `quick-start.ps1` | DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET |
| `quick-start-with-db.ps1` | Same as above |
| `setup-local.ps1` | All variables (interactive prompts) |
| `deploy.ps1` | Reads from Render environment |

---

## 🔄 Updating Environment Variables

### **After changing variables:**

1. **Local:** Restart backend application
2. **Render:** Automatic redeploy triggered
3. **Docker:** Restart containers: `docker-compose restart`
4. **Railway:** Click "Restart" button

### **Critical variables (require restart):**
- JWT_SECRET (invalidates all existing tokens!)
- DATABASE_URL
- APP_PORT

### **Non-critical (hot-reload):**
- UPLOADS_DIR (if supported by config)

---

## 📞 Support

If environment variables are not working:

1. Check logs: `docker logs backend` or Render logs
2. Verify format: No extra spaces, quotes, or special chars
3. Test locally first before deploying
4. Check application startup logs for "✅ Environment loaded"

---

**Last Updated:** January 4, 2026  
**Version:** 1.0.0
