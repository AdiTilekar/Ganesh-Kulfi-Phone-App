-- Day 13: Product Management Enhancements
-- Add isActive column for product activation/deactivation

-- Add isActive column to product table
ALTER TABLE product
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- Update existing products to active by default
UPDATE product
SET is_active = TRUE
WHERE is_active IS NULL;

-- Create index for faster filtering
CREATE INDEX IF NOT EXISTS idx_product_is_active ON product(is_active);

-- Create index for filtering active available products (retailer view)
CREATE INDEX IF NOT EXISTS idx_product_active_available ON product(is_active, is_available) WHERE is_active = TRUE AND is_available = TRUE;
