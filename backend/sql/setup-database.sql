-- Database Setup SQL for Ganesh Kulfi Backend
-- Run this in pgAdmin or your PostgreSQL client

-- Create database if not exists
CREATE DATABASE ganeshkulfi_db;

-- Connect to the database (in pgAdmin, switch to ganeshkulfi_db database)
-- Then run the following:

-- Create user
CREATE USER ganeshkulfi_user WITH PASSWORD 'kulfi@123';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE ganeshkulfi_db TO ganeshkulfi_user;
GRANT ALL ON SCHEMA public TO ganeshkulfi_user;
ALTER DATABASE ganeshkulfi_db OWNER TO ganeshkulfi_user;

-- Verify
SELECT 'Database setup complete!' as status;
