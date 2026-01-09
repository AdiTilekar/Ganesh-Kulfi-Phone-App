# Manual Database Setup Instructions

Since psql command-line access needs configuration, you have 2 easy options:

## Option 1: Use pgAdmin (Recommended)

1. Open **pgAdmin 4** (should be installed with PostgreSQL)
2. Connect to your PostgreSQL server
3. Right-click "Databases" → "Create" → "Database"
   - Database name: `ganeshkulfi_db`
   - Owner: postgres (for now)
4. Click Save
5. Right-click "Login/Group Roles" → "Create" → "Login/Group Role"
   - General tab - Name: `ganeshkulfi_user`
   - Definition tab - Password: `kulfi@123`
   - Privileges tab - Check: Can login, Create databases
6. Click Save
7. Right-click `ganeshkulfi_db` → Properties → General
   - Owner: Select `ganeshkulfi_user`
8. Click Save

**Done!** Now run: `.\quick-start-with-db.ps1`

---

## Option 2: Let the App Create Everything (Easiest!)

The backend will automatically create tables using Flyway migrations!

Just need database to exist:

### Using pgAdmin:
1. Open pgAdmin
2. Right-click "Databases" → "Create" → "Database"
3. Name: `ganeshkulfi_db`
4. Owner: postgres
5. Save

### Then edit `.env` file:
```env
DB_URL=jdbc:postgresql://localhost:5432/ganeshkulfi_db
DB_USER=postgres
DB_PASSWORD=YOUR_POSTGRES_PASSWORD
```

### Start backend:
```powershell
.\start-backend.ps1
```

The app will automatically:
- ✅ Connect to database
- ✅ Run all migrations (V1 through V11)
- ✅ Create all tables
- ✅ Insert sample product data
- ✅ Ready to use!

---

## Option 3: Simplest - Use Existing postgres User

1. Find your postgres password (you set it during PostgreSQL installation)
2. Create `.env` file:

```powershell
@"
DB_URL=jdbc:postgresql://localhost:5432/postgres
DB_USER=postgres
DB_PASSWORD=YOUR_ACTUAL_POSTGRES_PASSWORD
DB_POOL_SIZE=10
JWT_SECRET=$(-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 64 | % {[char]$_}))
JWT_ISSUER=ganeshkulfi
JWT_AUDIENCE=ganeshkulfi-app
APP_PORT=8080
UPLOADS_DIR=./uploads
"@ | Out-File -FilePath .env -Encoding UTF8
```

3. The backend will create a `ganeshkulfi_db` schema automatically in the postgres database!

---

## 🚀 Quick Start (Recommended)

**Just do this:**

1. **Open pgAdmin** → Create database `ganeshkulfi_db`
2. **Edit `.env`** → Set your actual postgres password
3. **Run:** `.\start-backend.ps1`
4. **Open browser:** http://localhost:8080
5. **Test!**

The backend handles everything else automatically! 🎉
