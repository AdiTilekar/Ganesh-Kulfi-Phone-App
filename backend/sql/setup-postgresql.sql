-- ============================================
-- PostgreSQL Database Setup Script
-- ============================================
-- This script creates the database and user for Ganesh Kulfi Backend
-- 
-- HOW TO RUN:
-- 1. Install PostgreSQL 14+ from https://www.postgresql.org/download/
-- 2. Open psql as postgres user:
--    psql -U postgres
-- 3. Run this file:
--    \i 'E:/Ganesh Kulfi web/KulfiDelightAndroid/backend/setup-postgresql.sql'
-- 
-- OR run from command line:
--    psql -U postgres -f "E:\Ganesh Kulfi web\KulfiDelightAndroid\backend\setup-postgresql.sql"
-- ============================================

-- Create database
DROP DATABASE IF EXISTS ganeshkulfi_db;
CREATE DATABASE ganeshkulfi_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Connect to the new database
\c ganeshkulfi_db

-- Create user with password
DROP USER IF EXISTS ganeshkulfi_user;
CREATE USER ganeshkulfi_user WITH PASSWORD 'Ganesh@123';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE ganeshkulfi_db TO ganeshkulfi_user;
GRANT ALL ON SCHEMA public TO ganeshkulfi_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ganeshkulfi_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ganeshkulfi_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO ganeshkulfi_user;

-- Enable UUID extension (required for V1 migration)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Display success message
\echo '✅ PostgreSQL database setup complete!'
\echo '📦 Database: ganeshkulfi_db'
\echo '👤 User: ganeshkulfi_user'
\echo '🔑 Password: Ganesh@123'
\echo '🔌 Connection: localhost:5432'
\echo ''
\echo 'Next steps:'
\echo '1. Start your backend: cd backend && ../gradlew run'
\echo '2. Flyway will automatically run all 4 migrations'
\echo '3. Test at: http://localhost:8080/api/health'
