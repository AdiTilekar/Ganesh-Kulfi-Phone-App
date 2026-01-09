-- Day 11: Order Timeline Tracking (No Firebase/FCM)
-- Add order timeline for tracking order status changes

-- 1. Create order_timeline table for tracking order status changes
CREATE TABLE IF NOT EXISTS order_timeline (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    message TEXT,
    created_by UUID REFERENCES app_user(id),
    created_by_role VARCHAR(20),
    notification_sent BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for order timeline
CREATE INDEX IF NOT EXISTS idx_order_timeline_order_id ON order_timeline(order_id);
CREATE INDEX IF NOT EXISTS idx_order_timeline_created_at ON order_timeline(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_timeline_status ON order_timeline(status);

-- 3. Add additional order statuses for detailed tracking
-- PENDING -> CONFIRMED -> PACKED -> OUT_FOR_DELIVERY -> DELIVERED
-- Note: Orders table already uses VARCHAR for status, so we don't need enum migration

-- 4. Create function to auto-create timeline entry on order status change
CREATE OR REPLACE FUNCTION create_order_timeline_entry()
RETURNS TRIGGER AS $$
BEGIN
    -- Only create timeline entry if status actually changed
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO order_timeline (
            order_id,
            status,
            message,
            created_by,
            created_by_role,
            created_at
        ) VALUES (
            NEW.id,
            NEW.status,
            CASE NEW.status
                WHEN 'CONFIRMED' THEN 'Order has been confirmed by factory'
                WHEN 'PACKED' THEN 'Order has been packed and ready for dispatch'
                WHEN 'OUT_FOR_DELIVERY' THEN 'Order is out for delivery'
                WHEN 'DELIVERED' THEN 'Order has been delivered successfully'
                WHEN 'CANCELLED' THEN 'Order has been cancelled'
                ELSE 'Order status updated to ' || NEW.status
            END,
            NEW.confirmed_by,
            CASE 
                WHEN NEW.confirmed_by IS NOT NULL THEN 'ADMIN'
                ELSE 'SYSTEM'
            END,
            NOW()
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 5. Create trigger for automatic timeline creation
DROP TRIGGER IF EXISTS trigger_create_order_timeline ON orders;
CREATE TRIGGER trigger_create_order_timeline
    AFTER UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION create_order_timeline_entry();

-- 6. Insert initial timeline entries for existing orders
INSERT INTO order_timeline (order_id, status, message, created_at)
SELECT 
    id,
    status,
    'Order placed',
    created_at
FROM orders
WHERE NOT EXISTS (
    SELECT 1 FROM order_timeline WHERE order_timeline.order_id = orders.id
);
