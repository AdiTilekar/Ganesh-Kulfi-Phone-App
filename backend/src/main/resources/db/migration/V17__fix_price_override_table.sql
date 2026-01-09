-- V17: Fix price_override table to match simplified tier system
-- Drop and recreate with correct structure

-- Drop existing price_override table
DROP TABLE IF EXISTS price_override CASCADE;

-- Recreate with correct tier reference matching app_user.tier
CREATE TABLE price_override (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id VARCHAR(36) NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    retailer_tier retailer_tier NOT NULL,
    override_price NUMERIC(10, 2) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_price_override_product ON price_override(product_id);
CREATE INDEX IF NOT EXISTS idx_price_override_retailer_tier ON price_override(retailer_tier);
CREATE INDEX IF NOT EXISTS idx_price_override_active ON price_override(active);

-- Create trigger for updated_at
DROP TRIGGER IF EXISTS update_price_override_updated_at ON price_override;
CREATE TRIGGER update_price_override_updated_at
    BEFORE UPDATE ON price_override
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
