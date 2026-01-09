-- Create new users with simple passwords
-- Factory Owner password: "factory123"
-- Retailer password: "retailer123"

-- Update or insert Factory Owner (ADMIN role)
INSERT INTO app_user (id, email, password_hash, name, role, created_at, updated_at)
VALUES (
    uuid_generate_v4(),
    'factory@ganeshkulfi.com',
    '$2a$10$rZ1zM5FQ7jH3qX8wN9tYqeMFvKJ0QwZ5xK6vL9xB2aP8mN3dR4eS6',
    'Factory Owner',
    'ADMIN',
    NOW(),
    NOW()
) ON CONFLICT (email) DO UPDATE 
SET password_hash = '$2a$10$rZ1zM5FQ7jH3qX8wN9tYqeMFvKJ0QwZ5xK6vL9xB2aP8mN3dR4eS6',
    name = 'Factory Owner';

-- Update or insert Retailer
INSERT INTO app_user (id, email, password_hash, name, phone, role, retailer_id, shop_name, tier, created_at, updated_at)
VALUES (
    uuid_generate_v4(),
    'retailer@ganeshkulfi.com',
    '$2a$10$tY2vL5MQ8kJ4rX9wP0uZsFnGwLK1RxA6yL7wM0yC3bQ9oO4fS5gT7',
    'Retailer',
    '9876543210',
    'RETAILER',
    'RET001',
    'Sweet Dreams Kulfi Shop',
    'VIP',
    NOW(),
    NOW()
) ON CONFLICT (email) DO UPDATE 
SET password_hash = '$2a$10$tY2vL5MQ8kJ4rX9wP0uZsFnGwLK1RxA6yL7wM0yC3bQ9oO4fS5gT7',
    name = 'Retailer',
    phone = '9876543210',
    retailer_id = 'RET001',
    shop_name = 'Sweet Dreams Kulfi Shop',
    tier = 'VIP';

SELECT 'Users created/updated!' as status;
SELECT email, name, role, retailer_id FROM app_user;
