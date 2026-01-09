-- V13: Update admin password and ensure user exists
-- Password: Admin1234 (BCrypt hash)

-- Update or insert admin user with correct password
INSERT INTO app_user (id, email, password_hash, name, role, created_at, updated_at)
VALUES (
    uuid_generate_v4(),
    'admin@ganeshkulfi.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYVKK6RZZ3i',
    'Admin',
    'ADMIN',
    NOW(),
    NOW()
) ON CONFLICT (email) DO UPDATE 
SET password_hash = '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYVKK6RZZ3i',
    name = 'Admin',
    role = 'ADMIN';
