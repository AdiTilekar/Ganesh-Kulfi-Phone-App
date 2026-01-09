-- =====================================================
-- Day 8: Order Module Enhancement - Production Ready
-- =====================================================
-- This migration adds:
-- 1. Idempotency support for order creation
-- 2. Confirmation message from admin to retailer
-- 3. Enhanced order tracking

-- Add idempotency_key to orders table for duplicate prevention
ALTER TABLE orders ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);

-- Create unique index on idempotency_key (where not null)
CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_idempotency_key 
    ON orders(idempotency_key) 
    WHERE idempotency_key IS NOT NULL;

-- Add confirmation_message for admin to communicate with retailer
ALTER TABLE orders ADD COLUMN IF NOT EXISTS confirmation_message TEXT;

-- Add index on idempotency_key for faster lookups
CREATE INDEX IF NOT EXISTS idx_orders_idempotency_lookup 
    ON orders(idempotency_key);

-- Add comment for documentation
COMMENT ON COLUMN orders.idempotency_key IS 
    'Unique key sent by client to prevent duplicate order creation on retry';

COMMENT ON COLUMN orders.confirmation_message IS 
    'Message from admin to retailer about order status (e.g., confirmation details or rejection reason)';

-- Function to clean up old idempotency keys (optional, for maintenance)
CREATE OR REPLACE FUNCTION cleanup_old_idempotency_keys()
RETURNS void AS $$
BEGIN
    -- Remove idempotency keys from orders older than 30 days
    -- This allows the same key to be reused after a month
    UPDATE orders
    SET idempotency_key = NULL
    WHERE created_at < NOW() - INTERVAL '30 days'
      AND idempotency_key IS NOT NULL;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_old_idempotency_keys() IS 
    'Maintenance function to clean up old idempotency keys (run periodically)';
