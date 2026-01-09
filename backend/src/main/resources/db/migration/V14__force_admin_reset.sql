-- V14: Force update admin password with fresh BCrypt hash
-- Password: Admin@123 (verified working hash)
-- Generated with BCrypt cost=12 and verified

-- Update password only (can't delete due to foreign key references in inventory_tx)
UPDATE app_user
SET password_hash = '$2a$12$V9jpW/99yOtPUh.WjtagIe.7zUQQaaf6rTFS9JhW7GwYtNGH33OaC',
    name = 'Admin',
    role = 'ADMIN',
    updated_at = NOW()
WHERE email = 'admin@ganeshkulfi.com';
