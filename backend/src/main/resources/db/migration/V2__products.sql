-- V2: Products table and initial kulfi flavors
-- Created: 2025-11-13
-- Description: Product catalog for Ganesh Kulfi with 13 kulfi flavors (exact match from Android app)

CREATE TABLE IF NOT EXISTS product (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    base_price NUMERIC(10, 2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    image_url VARCHAR(255),
    is_available BOOLEAN DEFAULT TRUE,
    is_seasonal BOOLEAN DEFAULT FALSE,
    stock_quantity INTEGER DEFAULT 0,
    min_order_quantity INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_category ON product(category);
CREATE INDEX IF NOT EXISTS idx_available ON product(is_available);
CREATE INDEX IF NOT EXISTS idx_name ON product(name);

-- Create trigger for product updated_at
DROP TRIGGER IF EXISTS update_product_updated_at ON product;
CREATE TRIGGER update_product_updated_at
    BEFORE UPDATE ON product
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert 13 kulfi flavors (exactly matching the Android app Flavor.kt)
INSERT INTO product (id, name, description, base_price, category, image_url, is_available, is_seasonal, stock_quantity) VALUES
('mango', 'Mango Kulfi', 'A seasonal delight capturing the sweet, luscious taste of ripe mangoes.', 20.00, 'FRUIT', 'mango_kulfi.png', TRUE, TRUE, 100),
('rabdi', 'Rabadi Kulfi', 'Rich, thickened milk kulfi with layers of creamy rabadi.', 20.00, 'CLASSIC', 'rabdi_kulfi.png', TRUE, FALSE, 80),
('strawberry', 'Strawberry Kulfi', 'A sweet and tangy kulfi made with fresh strawberries.', 25.00, 'FRUIT', 'strawberry_kulfi.png', TRUE, FALSE, 90),
('chocolate', 'Chocolate Kulfi', 'A modern twist with rich, decadent chocolate for all ages.', 35.00, 'PREMIUM', 'chocolate_kulfi.png', TRUE, FALSE, 120),
('paan', 'Paan Kulfi', 'Traditional paan flavor with a perfect blend of betel leaf essence.', 25.00, 'FUSION', 'paan_kulfi.png', TRUE, FALSE, 70),
('gulkand', 'Gulkand Kulfi', 'Aromatic rose petal preserve kulfi with a royal touch.', 30.00, 'PREMIUM', 'gulkand_kulfi.png', TRUE, FALSE, 60),
('dry_fruit', 'Dry Fruit Kulfi', 'Loaded with premium cashews, almonds, and pistachios.', 40.00, 'PREMIUM', 'dry_fruit_kulfi.png', TRUE, FALSE, 85),
('pineapple', 'Pineapple Kulfi', 'Tropical pineapple flavor with a refreshing tangy twist.', 25.00, 'FRUIT', 'pineapple_kulfi.png', TRUE, FALSE, 75),
('chikoo', 'Chikoo Kulfi', 'Sweet and smooth sapota kulfi with natural sweetness.', 22.00, 'FRUIT', 'chikoo_kulfi.png', TRUE, FALSE, 65),
('guava', 'Guava Kulfi', 'Fresh guava kulfi with authentic desi fruit flavor.', 22.00, 'FRUIT', 'guava_kulfi.png', TRUE, FALSE, 70),
('jamun', 'Jamun Kulfi', 'Rich purple jamun kulfi with tangy berry notes.', 28.00, 'FRUIT', 'jamun_kulfi.png', TRUE, FALSE, 55),
('sitafal', 'Sitafal Kulfi', 'Creamy custard apple kulfi with natural fruit chunks.', 32.00, 'FRUIT', 'sitafal_kulfi.png', TRUE, FALSE, 50),
('fig', 'Fig Kulfi', 'Premium fig kulfi with rich, honey-like sweetness.', 38.00, 'PREMIUM', 'fig_kulfi.png', TRUE, FALSE, 45);

-- Verification queries
SELECT '✅ Products table created successfully!' as status;
SELECT COUNT(*) as total_products FROM product;
SELECT name, base_price, category, is_seasonal FROM product ORDER BY category, name;
