# Database Backup Strategy

## 📦 Automated Backup Solution

### **1. Render.com PostgreSQL Backups**

Render provides automatic backups for PostgreSQL databases on paid plans.

**Free Tier:** No automatic backups  
**Paid Plans:** Daily automated backups with point-in-time recovery

**Enable on Render:**
1. Upgrade database to Starter plan ($7/month)
2. Backups automatically enabled
3. 7-day retention on Starter
4. 30-day retention on Pro+

**Manual Backup on Render:**
```bash
# From Render dashboard:
# Database → Settings → Manual Backup → Create Backup
```

---

### **2. Manual Backup Script (PowerShell)**

Create `backend/backup-database.ps1`:

```powershell
# Database Backup Script
# Exports PostgreSQL database to timestamped SQL file

param(
    [string]$OutputDir = ".\backups"
)

$timestamp = Get-Date -Format "yyyy-MM-dd_HHmmss"
$filename = "ganeshkulfi_backup_$timestamp.sql"
$outputPath = Join-Path $OutputDir $filename

# Create backup directory if it doesn't exist
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

Write-Host "🗄️  Creating database backup..." -ForegroundColor Cyan

# Option 1: From local database
if ($env:DB_URL) {
    $dbUrl = $env:DB_URL -replace "jdbc:", ""
    $dbUrl = $dbUrl -replace "postgresql://", ""
    $parts = $dbUrl -split "/"
    $hostPort = $parts[0]
    $dbName = $parts[1]
    $host, $port = $hostPort -split ":"
    
    pg_dump -h $host -p $port -U $env:DB_USER -d $dbName -F c -f $outputPath
}
# Option 2: From Render DATABASE_URL
elseif ($env:DATABASE_URL) {
    # Parse postgresql://user:pass@host:port/db
    pg_dump $env:DATABASE_URL -F c -f $outputPath
}
else {
    Write-Host "❌ No database URL found!" -ForegroundColor Red
    Write-Host "   Set DB_URL or DATABASE_URL environment variable" -ForegroundColor Yellow
    exit 1
}

if ($LASTEXITCODE -eq 0) {
    $size = (Get-Item $outputPath).Length / 1MB
    Write-Host "✅ Backup created successfully!" -ForegroundColor Green
    Write-Host "   File: $outputPath" -ForegroundColor White
    Write-Host "   Size: $([math]::Round($size, 2)) MB" -ForegroundColor White
} else {
    Write-Host "❌ Backup failed!" -ForegroundColor Red
}
```

**Usage:**
```powershell
# Backup to default directory (./backups)
.\backup-database.ps1

# Backup to specific directory
.\backup-database.ps1 -OutputDir "D:\backups"
```

---

### **3. Restore Script (PowerShell)**

Create `backend/restore-database.ps1`:

```powershell
# Database Restore Script
# Restores PostgreSQL database from backup file

param(
    [Parameter(Mandatory=$true)]
    [string]$BackupFile
)

if (-not (Test-Path $BackupFile)) {
    Write-Host "❌ Backup file not found: $BackupFile" -ForegroundColor Red
    exit 1
}

Write-Host "⚠️  WARNING: This will overwrite the current database!" -ForegroundColor Yellow
$confirm = Read-Host "Type 'YES' to confirm"

if ($confirm -ne "YES") {
    Write-Host "❌ Restore cancelled" -ForegroundColor Red
    exit 1
}

Write-Host "🔄 Restoring database..." -ForegroundColor Cyan

# Restore from backup
if ($env:DB_URL) {
    $dbUrl = $env:DB_URL -replace "jdbc:", ""
    $dbUrl = $dbUrl -replace "postgresql://", ""
    $parts = $dbUrl -split "/"
    $hostPort = $parts[0]
    $dbName = $parts[1]
    $host, $port = $hostPort -split ":"
    
    pg_restore -h $host -p $port -U $env:DB_USER -d $dbName -c $BackupFile
} elseif ($env:DATABASE_URL) {
    pg_restore $env:DATABASE_URL -c $BackupFile
} else {
    Write-Host "❌ No database URL found!" -ForegroundColor Red
    exit 1
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Database restored successfully!" -ForegroundColor Green
} else {
    Write-Host "❌ Restore failed!" -ForegroundColor Red
}
```

**Usage:**
```powershell
# Restore from backup file
.\restore-database.ps1 -BackupFile ".\backups\ganeshkulfi_backup_2026-01-04_120000.sql"
```

---

### **4. Automated Daily Backups (Task Scheduler)**

**Windows Task Scheduler:**

```powershell
# Create scheduled task for daily backups
$action = New-ScheduledTaskAction -Execute "PowerShell.exe" `
    -Argument "-File C:\ganeshkulfi\backend\backup-database.ps1"
$trigger = New-ScheduledTaskTrigger -Daily -At 2am
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable
$principal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -RunLevel Highest

Register-ScheduledTask -TaskName "GaneshKulfiBackup" `
    -Action $action `
    -Trigger $trigger `
    -Settings $settings `
    -Principal $principal
```

**Linux Cron Job:**

```bash
# Edit crontab
crontab -e

# Add daily backup at 2 AM
0 2 * * * cd /path/to/backend && ./backup-database.sh
```

---

### **5. Cloud Storage Integration**

**Upload backups to cloud after creation:**

```powershell
# Add to backup-database.ps1 after successful backup:

# Upload to AWS S3
aws s3 cp $outputPath s3://ganeshkulfi-backups/$filename

# Upload to Google Cloud Storage
gsutil cp $outputPath gs://ganeshkulfi-backups/$filename

# Upload to Azure Blob Storage
az storage blob upload --account-name ganeshkulfi `
    --container-name backups --file $outputPath --name $filename
```

---

## 📋 Backup Retention Policy

| Backup Type | Retention | Frequency | Storage |
|-------------|-----------|-----------|---------|
| **Hourly** | 24 hours | Every hour | Local only |
| **Daily** | 7 days | 2 AM daily | Local + Cloud |
| **Weekly** | 4 weeks | Sunday 2 AM | Cloud only |
| **Monthly** | 12 months | 1st of month | Cloud archive |

**Implementation:**
```powershell
# Cleanup old backups (keep last 7 days)
Get-ChildItem .\backups\*.sql | 
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-7) } | 
    Remove-Item
```

---

## 🔐 Backup Security

### **Encrypt Backups:**

```bash
# Encrypt backup with GPG
gpg --symmetric --cipher-algo AES256 backup.sql

# Decrypt when restoring
gpg --decrypt backup.sql.gpg > backup.sql
```

### **Access Control:**
- Store backups in private S3 bucket
- Use IAM roles with minimal permissions
- Enable versioning on cloud storage
- Encrypt at rest and in transit

---

## 🧪 Test Restore Process

**Monthly restore test:**

```powershell
# 1. Backup production
.\backup-database.ps1

# 2. Create test database
psql -c "CREATE DATABASE ganeshkulfi_test;"

# 3. Restore to test database
pg_restore -d ganeshkulfi_test backup.sql

# 4. Verify data integrity
psql -d ganeshkulfi_test -c "SELECT COUNT(*) FROM app_user;"
psql -d ganeshkulfi_test -c "SELECT COUNT(*) FROM orders;"

# 5. Cleanup
psql -c "DROP DATABASE ganeshkulfi_test;"
```

---

## 📞 Disaster Recovery

### **Complete System Failure:**

1. **Provision new database** on Render/AWS/GCP
2. **Get latest backup** from cloud storage
3. **Restore database:**
   ```bash
   pg_restore -d new_database backup.sql
   ```
4. **Update DATABASE_URL** in backend environment
5. **Verify Flyway migrations** applied correctly
6. **Test application** thoroughly
7. **Update DNS** to point to new backend

**Recovery Time Objective (RTO):** < 1 hour  
**Recovery Point Objective (RPO):** Last backup (24 hours max)

---

## 🔍 Backup Monitoring

**Check backup status:**

```powershell
# List recent backups
Get-ChildItem .\backups\*.sql | 
    Select-Object Name, Length, LastWriteTime | 
    Sort-Object LastWriteTime -Descending | 
    Format-Table

# Verify backup integrity
pg_restore --list backup.sql
```

**Alert on backup failure:**

```powershell
# Add to backup script:
if ($LASTEXITCODE -ne 0) {
    # Send alert email
    Send-MailMessage -To "admin@ganeshkulfi.com" `
        -Subject "Backup Failed!" `
        -Body "Database backup failed at $(Get-Date)"
}
```

---

## 📚 Additional Resources

- **PostgreSQL Backup Docs:** https://www.postgresql.org/docs/current/backup.html
- **Render Backups:** https://render.com/docs/postgresql#backups
- **pg_dump Reference:** https://www.postgresql.org/docs/current/app-pgdump.html
- **pg_restore Reference:** https://www.postgresql.org/docs/current/app-pgrestore.html

---

**Last Updated:** January 4, 2026  
**Next Review:** February 4, 2026
