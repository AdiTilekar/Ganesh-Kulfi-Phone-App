-- Day 10: Order Status History Table for Audit Trail
-- Track all status changes for orders with who made the change and why

-- 1. Add new order statuses to support cancellation
-- Current statuses: PENDING, CONFIRMED, REJECTED, COMPLETED, CANCELLED
-- We need to add: CANCELLED_ADMIN (when admin cancels an order)
-- Note: Orders table already has status as varchar, so no migration needed for enum

-- 2. Create order_status_history table
CREATE TABLE IF NOT EXISTS order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by UUID,  -- UUID of user who made the change (nullable for system changes)
    changed_by_role VARCHAR(20) NOT NULL,  -- ADMIN, RETAILER, SYSTEM
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Create indexes for fast lookups
CREATE INDEX idx_order_status_history_order_id ON order_status_history(order_id);
CREATE INDEX idx_order_status_history_created_at ON order_status_history(created_at DESC);
CREATE INDEX idx_order_status_history_changed_by ON order_status_history(changed_by);

-- 4. Add cancelled_at timestamp to orders table
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS cancelled_by UUID REFERENCES app_user(id),
ADD COLUMN IF NOT EXISTS cancellation_reason TEXT;

-- 5. Function to automatically record status changes
CREATE OR REPLACE FUNCTION record_order_status_change()
RETURNS TRIGGER AS $$
BEGIN
    -- Only record if status actually changed
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO order_status_history (
            order_id,
            old_status,
            new_status,
            changed_by,
            changed_by_role,
            reason,
            created_at
        ) VALUES (
            NEW.id,
            OLD.status::VARCHAR,
            NEW.status::VARCHAR,
            COALESCE(NEW.confirmed_by, NEW.rejected_by, NEW.cancelled_by, NEW.retailer_id)::CHAR(36),
            CASE 
                WHEN NEW.confirmed_by IS NOT NULL OR NEW.rejected_by IS NOT NULL OR NEW.cancelled_by IS NOT NULL THEN 'ADMIN'
                ELSE 'RETAILER'
            END,
            COALESCE(NEW.cancellation_reason, NEW.rejection_reason, NEW.factory_notes),
            CURRENT_TIMESTAMP
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 6. Create trigger to auto-record status changes
CREATE TRIGGER trigger_record_order_status_change
AFTER UPDATE ON orders
FOR EACH ROW
WHEN (OLD.status IS DISTINCT FROM NEW.status)
EXECUTE FUNCTION record_order_status_change();

-- 7. Populate initial history for existing orders (mark all as created by retailer initially)
INSERT INTO order_status_history (order_id, old_status, new_status, changed_by, changed_by_role, created_at)
SELECT 
    id,
    NULL,
    status::VARCHAR,
    retailer_id,
    'RETAILER',
    created_at
FROM orders;

COMMENT ON TABLE order_status_history IS 'Audit trail of all order status changes. Used for admin dashboard and compliance.';
COMMENT ON COLUMN order_status_history.changed_by IS 'UUID of the user (admin or retailer) who changed the status';
COMMENT ON COLUMN order_status_history.changed_by_role IS 'Role of the user who made the change (ADMIN or RETAILER)';
COMMENT ON COLUMN orders.cancelled_at IS 'Timestamp when order was cancelled (by retailer or admin)';
COMMENT ON COLUMN orders.cancelled_by IS 'User ID who cancelled the order (admin or retailer)';
