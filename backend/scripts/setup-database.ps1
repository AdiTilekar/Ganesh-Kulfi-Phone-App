# Database Setup for Local Testing
# Creates database and user for Ganesh Kulfi Backend
# ⚠️  SECURITY WARNING: Uses default password 'kulfi@123'
# ⚠️  Change this password immediately for any deployment!

Write-Host "🗄️  Ganesh Kulfi - Database Setup" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  WARNING: This script uses a weak default password!" -ForegroundColor Red
Write-Host "⚠️  Password: kulfi@123 - For local testing only!" -ForegroundColor Red
Write-Host "" -ForegroundColor Red

Write-Host "This script will create:" -ForegroundColor Yellow
Write-Host "   • Database: ganeshkulfi_db" -ForegroundColor Gray
Write-Host "   • User: ganeshkulfi_user" -ForegroundColor Gray
Write-Host "   • Password: kulfi@123 (change after testing)" -ForegroundColor Gray
Write-Host ""

$postgresPassword = Read-Host "Enter your PostgreSQL 'postgres' user password" -AsSecureString
$postgresPasswordPlain = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($postgresPassword)
)

Write-Host ""
Write-Host "Creating database and user..." -ForegroundColor Yellow

# SQL commands to create database and user
$sql = @"
-- Create database
SELECT 'CREATE DATABASE ganeshkulfi_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ganeshkulfi_db')\gexec

-- Connect to the database
\c ganeshkulfi_db

-- Create user if not exists
DO
\$\$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles WHERE rolname = 'ganeshkulfi_user') THEN
      CREATE USER ganeshkulfi_user WITH PASSWORD 'kulfi@123';
   END IF;
END
\$\$;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE ganeshkulfi_db TO ganeshkulfi_user;
GRANT ALL ON SCHEMA public TO ganeshkulfi_user;
ALTER DATABASE ganeshkulfi_db OWNER TO ganeshkulfi_user;

-- Show success
SELECT 'Database setup complete!' as status;
"@

# Save SQL to temporary file
$sqlFile = "temp_setup.sql"
$sql | Out-File -FilePath $sqlFile -Encoding UTF8

# Run SQL
$env:PGPASSWORD = $postgresPasswordPlain
psql -U postgres -h localhost -p 5432 -f $sqlFile

# Clean up
Remove-Item $sqlFile -ErrorAction SilentlyContinue
$env:PGPASSWORD = $null

Write-Host ""
Write-Host "✅ Database setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Credentials:" -ForegroundColor Cyan
Write-Host "   Database: ganeshkulfi_db" -ForegroundColor White
Write-Host "   User: ganeshkulfi_user" -ForegroundColor White
Write-Host "   Password: kulfi@123" -ForegroundColor White
Write-Host ""
Write-Host "Next step: Run .\quick-start-with-db.ps1" -ForegroundColor Yellow
Write-Host ""
