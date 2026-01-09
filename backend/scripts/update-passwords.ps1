# PowerShell script to update passwords using psql with PGPASSWORD environment variable

$env:PGPASSWORD = "Ganesh@123"

& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U ganeshkulfi_user -d ganeshkulfi_db -h localhost -c @"
-- Update or insert factory owner
INSERT INTO app_user (id, email, password_hash, name, role, phone, shop_name, tier, created_at, updated_at)
VALUES (
    uuid_generate_v4(),
    'factory@ganeshkulfi.com',
    '$2a$10$rZ1zM5FQ7jH3qX8wN9tYqeMFvKJ0QwZ5xK6vL9xB2aP8mN3dR4eS6',
    'Factory Owner',
    'ADMIN',
    NULL,
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    name = EXCLUDED.name,
    role = EXCLUDED.role,
    updated_at = CURRENT_TIMESTAMP;

-- Update or insert retailer
INSERT INTO app_user (id, email, password_hash, name, role, phone, shop_name, tier, created_at, updated_at)
VALUES (
    uuid_generate_v4(),
    'retailer@ganeshkulfi.com',
    '$2a$10$tY2vL5MQyN8xK2bP9wE3tOzM6aR4sH7vN5cF2eW9xK1bT8mL4pR6a',
    'Retailer',
    'RETAILER',
    '9876543210',
    'Sweet Dreams',
    'VIP',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO UPDATE
SET password_hash = EXCLUDED.password_hash,
    name = EXCLUDED.name,
    role = EXCLUDED.role,
    phone = EXCLUDED.phone,
    shop_name = EXCLUDED.shop_name,
    tier = EXCLUDED.tier,
    updated_at = CURRENT_TIMESTAMP;
"@

# Clear password from environment
Remove-Item Env:PGPASSWORD

Write-Host "`n=== Passwords Updated Successfully ===" -ForegroundColor Green
Write-Host "Factory Owner: factory@ganeshkulfi.com / factory123" -ForegroundColor Cyan
Write-Host "Retailer: retailer@ganeshkulfi.com / retailer123" -ForegroundColor Cyan
