-- Ganesh Kulfi Backend - Initial PostgreSQL Schema
-- Version: 1 (PostgreSQL)
-- Description: Creates user table with roles and pricing tiers

-- Create ENUM types for role and tier
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('ADMIN', 'RETAILER', 'CUSTOMER', 'GUEST');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE pricing_tier AS ENUM ('VIP', 'PREMIUM', 'REGULAR', 'RETAIL');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create app_user table
CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role user_role NOT NULL DEFAULT 'CUSTOMER',
    retailer_id VARCHAR(50) UNIQUE,
    shop_name VARCHAR(255),
    tier pricing_tier,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_email ON app_user(email);
CREATE INDEX IF NOT EXISTS idx_role ON app_user(role);
CREATE INDEX IF NOT EXISTS idx_retailer_id ON app_user(retailer_id);

-- Create trigger function to auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for app_user
DROP TRIGGER IF EXISTS update_app_user_updated_at ON app_user;
CREATE TRIGGER update_app_user_updated_at
    BEFORE UPDATE ON app_user
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert default admin user
-- Password: Admin1234 (bcrypt hash)
INSERT INTO app_user (id, email, password_hash, name, role, created_at, updated_at)
VALUES (
    uuid_generate_v4(),
    'admin@ganeshkulfi.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYVKK6RZZ3i',
    'Admin',
    'ADMIN',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

-- Insert default retailer user
-- Password: Retailer1234 (bcrypt hash)
INSERT INTO app_user (id, email, password_hash, name, phone, role, retailer_id, shop_name, tier, created_at, updated_at)
VALUES (
    uuid_generate_v4(),
    'retailer@test.com',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'Test Retailer',
    '9876543210',
    'RETAILER',
    'RET001',
    'Sweet Dreams Kulfi Shop',
    'VIP',
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;
