-- Day 9: Advanced Pricing System (Server-Side Only)
-- Add retailer tier enum and price override table

-- 1. Create retailer tier enum (only 3 tiers: BASIC, SILVER, GOLD)
DO $$ BEGIN
    CREATE TYPE retailer_tier AS ENUM ('BASIC', 'SILVER', 'GOLD');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- 2. Convert existing tier column from old pricing_tier enum to varchar
-- First, change to varchar to avoid enum conflicts
ALTER TABLE app_user ALTER COLUMN tier TYPE varchar(20);

-- Map old pricing_tier values to new retailer_tier values (3 tiers only)
-- Old: VIP, PREMIUM, REGULAR, RETAIL, PLATINUM -> New: BASIC, SILVER, GOLD
UPDATE app_user SET tier = CASE 
    WHEN tier IN ('VIP', 'PLATINUM') THEN 'GOLD'
    WHEN tier IN ('PREMIUM', 'REGULAR') THEN 'SILVER'
    WHEN tier IN ('RETAIL', 'WHOLESALE', 'CUSTOM') THEN 'BASIC'
    ELSE 'BASIC'
END;

-- 3. Create price_override table
CREATE TABLE IF NOT EXISTS price_override (
    id SERIAL PRIMARY KEY,
    product_id CHAR(36) NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    tier retailer_tier NOT NULL,
    override_price NUMERIC(10, 2) NOT NULL CHECK (override_price >= 0),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_id, tier)
);

-- 4. Create index for fast lookups
CREATE INDEX idx_price_override_product_tier ON price_override(product_id, tier) WHERE active = true;

-- 5. Update orders table to store price breakdown (server-side only)
ALTER TABLE orders
ADD COLUMN base_price NUMERIC(10, 2),
ADD COLUMN override_price NUMERIC(10, 2),
ADD COLUMN discount_percentage NUMERIC(5, 2) DEFAULT 0,
ADD COLUMN gst_percentage NUMERIC(5, 2) DEFAULT 18;

-- 6. Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_price_override_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 7. Trigger to auto-update updated_at
CREATE TRIGGER trigger_price_override_timestamp
BEFORE UPDATE ON price_override
FOR EACH ROW
EXECUTE FUNCTION update_price_override_timestamp();

-- 8. Insert sample data for testing (optional - only for development)
-- Uncomment below if you want test data

-- Update some test users to have different tiers
-- UPDATE app_user SET tier = 'SILVER' WHERE id = 2;
-- UPDATE app_user SET tier = 'GOLD' WHERE id = 3;
-- UPDATE app_user SET tier = 'PLATINUM' WHERE id = 4;

-- Create some price overrides for testing
-- INSERT INTO price_override (product_id, tier, override_price) VALUES
-- (1, 'SILVER', 95.00),
-- (1, 'GOLD', 90.00),
-- (1, 'PLATINUM', 85.00),
-- (2, 'SILVER', 45.00),
-- (2, 'GOLD', 42.00),
-- (2, 'PLATINUM', 40.00);

COMMENT ON TABLE price_override IS 'Server-side only price overrides per product and retailer tier. NEVER exposed to retailers.';
COMMENT ON COLUMN app_user.tier IS 'Retailer tier for pricing calculation. Server-side only, hidden from retailer API responses.';
COMMENT ON COLUMN orders.base_price IS 'Product base price at time of order (server-side tracking only)';
COMMENT ON COLUMN orders.override_price IS 'Tier-specific override price at time of order (server-side tracking only)';
COMMENT ON COLUMN orders.discount_percentage IS 'Quantity discount percentage applied (server-side tracking only)';
COMMENT ON COLUMN orders.gst_percentage IS 'GST percentage applied at time of order (server-side tracking only)';
